package org.jboss.set.mavendependencyupdater;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.common.model.Project;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.loggerclient.ComponentUpgradeDTO;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClient;
import org.jboss.set.mavendependencyupdater.loggerclient.UpgradeNotFoundException;
import org.jboss.set.mavendependencyupdater.rules.NeverRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.TokenizedVersion;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;
import org.jboss.set.mavendependencyupdater.rules.VersionStreamRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DependencyEvaluator {

    private static final Logger LOG = Logger.getLogger(DependencyEvaluator.class);

    private static final VersionStreamRestriction DEFAULT_STREAM_RESTRICTION =
            new VersionStreamRestriction(VersionStream.MICRO);
    private static final Comparator<Version> VERSION_COMPARATOR = Comparator.comparing(v -> TokenizedVersion.parse(v.toString()));

    private final Configuration configuration;
    private final AvailableVersionsResolver availableVersionsResolver;
    private final LoggerClient loggerClient;
    private boolean configUpToDate;

    public DependencyEvaluator(Configuration configuration, AvailableVersionsResolver availableVersionsResolver,
                               LoggerClient loggerClient) {
        this.configuration = configuration;
        this.availableVersionsResolver = availableVersionsResolver;
        this.loggerClient = loggerClient;
    }

    /**
     * Determine which artifacts can be upgraded.
     *
     * @return returns map "G:A" => Pair[newVersion, repoUrl]
     */
    public List<ArtifactResult<ComponentUpgrade>> getVersionsToUpgrade(Map<Project, Collection<ScopedArtifactRef>> dependencies) {
        List<ArtifactResult<ComponentUpgrade>> versionsToUpgrade = new ArrayList<>();

        for (Map.Entry<Project, Collection<ScopedArtifactRef>> entry: dependencies.entrySet()) {
            versionsToUpgrade.addAll(getVersionsToUpgrade(entry.getKey(), entry.getValue()));
        }

        return versionsToUpgrade;
    }

    public List<ArtifactResult<ComponentUpgrade>> getVersionsToUpgrade(Collection<ScopedArtifactRef> dependencies) {
        return getVersionsToUpgrade(null, dependencies);
    }

    private List<ArtifactResult<ComponentUpgrade>> getVersionsToUpgrade(Project project, Collection<ScopedArtifactRef> dependencies) {
        List<ArtifactResult<ComponentUpgrade>> versionsToUpgrade = new ArrayList<>();
        configUpToDate = true;

        LOG.infof("Going through project :%s dependencies.", project != null ? project.getArtifactId() : "null");

        for (ScopedArtifactRef dep : dependencies) {
            Artifact rangeArtifact = toVersionRangeArtifact(dep);

            if (dep.getVersionString().startsWith("$")) {
                LOG.warnf("Skipping dependency '%s', should this be resolved?", dep);
                continue;
            }

            if (configuration.getIgnoreScopes().contains(dep.getScope())) {
                LOG.debugf("Skipping dependency '%s', scope '%s' is ignored", dep, dep.getScope());
                continue;
            }

            try {
                List<Restriction> restrictions =
                        configuration.getRestrictionsFor(dep.getGroupId(), dep.getArtifactId());

                VersionRangeResult versionRangeResult = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                ArtifactResult<Version> scopedVersions = findLatest(dep, restrictions, versionRangeResult.getVersions());

                LOG.debugf("Available versions for '%s': %s", dep, versionRangeResult);
                if (scopedVersions.anyPresent()) {
                    ComponentUpgrade latestConfigured = null, latestMinor = null, veryLatest = null;
                    if (versionDiffersFromCurrent(dep.getVersionString(), scopedVersions.getLatestConfigured())) {
                        latestConfigured = upgradeInfo(scopedVersions.getLatestConfigured(), versionRangeResult, dep, project);
                    }
                    if (versionDiffersFromCurrent(dep.getVersionString(), scopedVersions.getLatestMinor())) {
                        latestMinor = upgradeInfo(scopedVersions.getLatestMinor(), versionRangeResult, dep, project);
                    }
                    if (versionDiffersFromCurrent(dep.getVersionString(), scopedVersions.getVeryLatest())) {
                        veryLatest = upgradeInfo(scopedVersions.getVeryLatest(), versionRangeResult, dep, project);
                    }

                    versionsToUpgrade.add(new ArtifactResult<>(dep, latestConfigured, latestMinor, veryLatest));
                } else {
                    LOG.debugf("  => no change");
                }
            } catch (RepositoryException e) {
                LOG.errorf("Could not resolve '%s'", rangeArtifact.toString());
            }
        }

        if (!configUpToDate) {
            LOG.warn("Configuration not up to date. Check the warnings above.");
        }

        sendDetectedUpgradesToExternalService(versionsToUpgrade);

        return versionsToUpgrade;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ComponentUpgrade upgradeInfo(Optional<Version> version, VersionRangeResult versionRangeResult, ScopedArtifactRef artifactRef, Project project) {
        if (version.isPresent()) {
            String repoId = versionRangeResult.getRepository(version.get()).getId();
            LOG.infof("Found possible upgrade of '%s' to '%s' in repo '%s'", artifactRef, version.get(), repoId);

            // check when this upgrade was first detected
            LocalDateTime firstSeen = findComponentUpgradeDate(artifactRef, version.get().toString());
            return new ComponentUpgrade(artifactRef, version.get().toString(), repoId, firstSeen, project);
        }
        return null;
    }

    private static Artifact toVersionRangeArtifact(ArtifactRef ref) {
        return new DefaultArtifact(ref.getGroupId(), ref.getArtifactId(), null, "[" + ref.getVersionString() + ",)");
    }


    /**
     * Searches given list of available versions for the latest version in given stream.
     *
     * @param dependency        original dependency.
     * @param restrictions      list of restrictions that versions must satisfy.
     *                          If `VersionPrefixRestriction` is present, `stream` parameter is ignored.
     * @param availableVersions available versions.
     * @return latest available version in given stream.
     */
    ArtifactResult<Version> findLatest(ScopedArtifactRef dependency,
                                       List<Restriction> restrictions,
                                       List<Version> availableVersions) {
        if (restrictions.stream().anyMatch(r -> r instanceof NeverRestriction)) {
            return ArtifactResult.empty(dependency); // blacklisted component
        }

        Optional<Restriction> prefixRestrictionOptional =
                restrictions.stream().filter(r -> r instanceof VersionPrefixRestriction).findFirst();
        final boolean restrictedPrefix = prefixRestrictionOptional.isPresent();
        boolean restrictedStream = restrictions.stream().anyMatch(r -> r instanceof VersionStreamRestriction);
        final String originalVersion = dependency.getVersionString();

        if (restrictedPrefix) {
            VersionPrefixRestriction prefixRestriction = (VersionPrefixRestriction) prefixRestrictionOptional.get();
            if (!prefixRestriction.applies(originalVersion, originalVersion)) {
                // configured version prefix doesn't match existing dependency => configuration needs to be updated
                LOG.warnf("Existing dependency '%s' doesn't match configured prefix '%s'. Configuration probably needs to be updated.",
                        dependency, prefixRestriction.getPrefixString());
                configUpToDate = false;
                return ArtifactResult.empty(dependency);
            }
        }

        // apply all configured restrictions to obtain "latest configured version"
        Stream<Version> latestConfiguredStream = availableVersions.stream();
        if (!restrictedPrefix && !restrictedStream) {
            latestConfiguredStream = latestConfiguredStream.filter(version -> DEFAULT_STREAM_RESTRICTION.applies(version.toString(), originalVersion));
        }
        for (Restriction restriction : restrictions) {
            latestConfiguredStream = latestConfiguredStream.filter(version -> restriction.applies(version.toString(), originalVersion));
        }
        Optional<Version> latestConfigured = latestConfiguredStream.max(VERSION_COMPARATOR)
                // must not be equal to current version
                .filter(version -> !version.toString().equals(dependency.getVersionString()));

        // remove all stream and prefix restrictions
        List<Restriction> restrictionSubset = restrictions.stream()
                .filter(r -> !(r instanceof VersionPrefixRestriction || r instanceof VersionStreamRestriction))
                .collect(Collectors.toList());
        // find latest in the same MAJOR
        Stream<Version> latestMinorStream = availableVersions.stream();
        for (Restriction restriction: restrictionSubset) {
            latestMinorStream = latestMinorStream.filter(version -> restriction.applies(version.toString(), originalVersion));
        }
        Optional<Version> latestMinor = latestMinorStream.filter(version -> new VersionStreamRestriction(VersionStream.MINOR).applies(version.toString(), originalVersion))
                .max(VERSION_COMPARATOR)
                // must not be equal to current version
                .filter(version -> !version.toString().equals(dependency.getVersionString()))
                // must not be equal to the previous value
                .filter(version -> !(latestConfigured.isPresent() && version.equals(latestConfigured.get())));

        // find very latest version
        Stream<Version> veryLatestStream = availableVersions.stream();
        for (Restriction restriction: restrictionSubset) {
            veryLatestStream = veryLatestStream.filter(version -> restriction.applies(version.toString(), originalVersion));
        }
        Optional<Version> veryLatest = veryLatestStream.max(VERSION_COMPARATOR)
                // must not be equal to current version
                .filter(version -> !version.toString().equals(dependency.getVersionString()))
                // must not be equal to one of previous values
                .filter(version -> !(latestConfigured.isPresent() && version.equals(latestConfigured.get()))
                        && !(latestMinor.isPresent() && version.equals(latestMinor.get())));

        return new ArtifactResult<>(dependency, latestConfigured, latestMinor, veryLatest);
    }

    LocalDateTime findComponentUpgradeDate(ScopedArtifactRef dep, String newVersion) {
        if (loggerClient != null) {
            try {
                ComponentUpgradeDTO componentUpgrade = loggerClient.getFirst(configuration.getLogger().getProjectCode(), dep.getGroupId(), dep.getArtifactId(), newVersion);
                if (componentUpgrade != null) {
                    LOG.infof("Component upgrade to %s:%s:%s already seen at %s", dep.getGroupId(), dep.getArtifactId(), newVersion, componentUpgrade.created);
                    return componentUpgrade.created;
                }
            } catch (UpgradeNotFoundException e) {
                LOG.infof("Component upgrade to %s:%s:%s was not previously recorded", dep.getGroupId(), dep.getArtifactId(), newVersion);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to obtain date when a component upgrade was first seen");
            }
        }
        return null;
    }

    void sendDetectedUpgradesToExternalService(List<ArtifactResult<ComponentUpgrade>> componentUpgrades) {
        if (loggerClient == null) {
            LOG.info("Logger not configured. Discovered component upgrades will not be recorded.");
            return;
        }
        if (componentUpgrades.isEmpty()) {
            LOG.info("No component upgrades to report.");
            return;
        }

        String projectCode = configuration.getLogger().getProjectCode();
        LOG.infof("Recording %d component upgrades under project '%s'", componentUpgrades.size(), projectCode);

        // send list of upgrades to the logger in batches, to prevent a POST request getting too large
        final int batchSize = 30;
        int fromIndex = 0;
        do {
            final int toIndex = Math.min(componentUpgrades.size(), fromIndex + batchSize);

            final List<ComponentUpgradeDTO> dtos = new ArrayList<>();
            for (ArtifactResult<ComponentUpgrade> upgrades: componentUpgrades.subList(fromIndex, toIndex)) {
                upgrades.getLatestConfigured().ifPresent(u -> dtos.add(convertToDTO(projectCode, u)));
                upgrades.getLatestMinor().ifPresent(u -> dtos.add(convertToDTO(projectCode, u)));
                upgrades.getVeryLatest().ifPresent(u -> dtos.add(convertToDTO(projectCode, u)));
            }

            if (LOG.isEnabled(Logger.Level.DEBUG)) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String json = objectMapper.writeValueAsString(dtos);
                    LOG.debugf("Sending detected upgrades to %s (json length: %d):\n%s",
                            configuration.getLogger().getUri(), json.length(), json);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            try {
                loggerClient.create(dtos);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send discovered component upgrades to the logger service.");
            }

            fromIndex += batchSize;
        } while (fromIndex < componentUpgrades.size());
    }

    static ComponentUpgradeDTO convertToDTO(String projectCode, ComponentUpgrade componentUpgrade) {
        return new ComponentUpgradeDTO(projectCode,
                componentUpgrade.getArtifact().getGroupId(),
                componentUpgrade.getArtifact().getArtifactId(),
                componentUpgrade.getArtifact().getVersionString(),
                componentUpgrade.getNewVersion(),
                null);
    }

    static boolean versionDiffersFromCurrent(String current, Optional<Version> candidate) {
        return candidate.isPresent() && !current.equals(candidate.get().toString());
    }

}
