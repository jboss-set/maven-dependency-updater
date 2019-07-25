package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.utils.VersionUtils.equalMmm;

public class DependencyEvaluator {

    private static final Logger LOG = Logger.getLogger(DependencyEvaluator.class);

    private Configuration configuration;
    private AvailableVersionsResolver availableVersionsResolver;
    private boolean configUpToDate;

    public DependencyEvaluator(Configuration configuration, AvailableVersionsResolver availableVersionsResolver) {
        this.configuration = configuration;
        this.availableVersionsResolver = availableVersionsResolver;
    }

    /**
     * Determine which artifacts can be upgraded.
     *
     * @return returns map G:A => newVersion
     */
    public Map<ArtifactRef, String> getVersionsToUpgrade(Collection<ScopedArtifactRef> dependencies) {
        Map<ArtifactRef, String> versionsToUpgrade = new HashMap<>();
        configUpToDate = true;

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
                VersionStream stream =
                        configuration.getStreamFor(dep.getGroupId(), dep.getArtifactId(), MICRO);
                List<Restriction> restrictions =
                        configuration.getRestrictionsFor(dep.getGroupId(), dep.getArtifactId());

                List<Version> versions = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                Optional<Version> latest =
                        findLatest(dep, stream, restrictions, versions);

                LOG.debugf("Available versions for '%s' %s: %s", dep, stream, versions);
                if (latest.isPresent()
                        && !dep.getVersionString().equals(latest.get().toString())) {
                    LOG.infof("Upgrading '%s' to '%s'", dep, latest.get().toString());
                    versionsToUpgrade.put(dep,
                            latest.get().toString());
                } else {
                    LOG.debugf("  => no change");
                }
            } catch (RepositoryException e) {
                LOG.errorf("Could not resolve '%s'", rangeArtifact.toString());
            }
        }

        if (!configUpToDate) {
            LOG.warn("Configuration not up to date? Check the warnings above.");
        }

        return versionsToUpgrade;
    }

    private static Artifact toVersionRangeArtifact(ArtifactRef ref) {
        return new DefaultArtifact(ref.getGroupId(), ref.getArtifactId(), null, "[" + ref.getVersionString() + ",)");
    }


    /**
     * Searches given list of available versions for the latest version in given stream.
     *
     * @param dependency        original dependency.
     * @param stream            the highest segment of the version that is allowed to change.
     *                          This parameter is ignored if `restrictions` list contains `VersionPrefixRestriction`.
     * @param restrictions      list of restrictions that versions must satisfy.
     *                          If `VersionPrefixRestriction` is present, `stream` parameter is ignored.
     * @param availableVersions available versions.
     * @return latest available version in given stream.
     */
    Optional<Version> findLatest(ScopedArtifactRef dependency,
                                               VersionStream stream,
                                               List<Restriction> restrictions,
                                               List<Version> availableVersions) {
        Optional<Restriction> prefixOptional =
                restrictions.stream().filter(r -> r instanceof VersionPrefixRestriction).findFirst();
        boolean restrictedPrefix = prefixOptional.isPresent();

        if (restrictedPrefix) {
            VersionPrefixRestriction prefixRestriction = (VersionPrefixRestriction) prefixOptional.get();
            if (!prefixRestriction.applies(dependency.getVersionString())) {
                LOG.warnf("Existing dependency '%s' doesn't match configured prefix: '%s'",
                        dependency, prefixRestriction.getPrefixString());
                configUpToDate = false;
                return Optional.empty();
            }
        }

        Stream<Version> workingStream = availableVersions.stream();

        if (!restrictedPrefix && !VersionStream.ANY.equals(stream)) { // don't filter by stream if prefix restriction is present
            workingStream = workingStream.filter(v -> equalMmm(dependency.getVersionString(), v.toString(), stream.higher()));
        }

        for (Restriction restriction : restrictions) {
            workingStream = workingStream.filter(v -> restriction.applies(v.toString()));
        }

        return workingStream.max(Comparator.naturalOrder());
    }

}
