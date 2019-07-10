package org.jboss.set.mavendependencyupdater;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.utils.VersionUtils;

public class DependencyEvaluator {

    private static final Logger LOG = Logger.getLogger(DependencyEvaluator.class);

    private Configuration configuration;
    private AvailableVersionsResolver availableVersionsResolver;

    public DependencyEvaluator(Configuration configuration, AvailableVersionsResolver availableVersionsResolver) {
        this.configuration = configuration;
        this.availableVersionsResolver = availableVersionsResolver;
    }

    /**
     * Determine which artifacts can be upgraded.
     *
     * @return returns map G:A => newVersion
     */
    public Map<String, String> getVersionsToUpgrade(Collection<String> dependencies) {
        Map<String, String> versionsToUpgrade = new HashMap<>();

        for (String gav : dependencies) {
            if (gav.isEmpty()) {
                continue;
            }

            Artifact artifact = newArtifact(gav);
            Artifact rangeArtifact = newVersionRangeArtifact(gav);

            if (artifact.getBaseVersion().startsWith("$")) {
                LOG.infof("Skipping %s", artifact);
                continue;
            }
            try {
                VersionStream stream =
                        configuration.getStreamFor(artifact.getGroupId(), artifact.getArtifactId(), MICRO);

                List<Version> versions = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                Optional<Version> latest = VersionUtils.findLatest(stream, artifact.getBaseVersion(), versions);

                LOG.infof("Available versions for %s %s: %s", gav, stream, versions);
                if (latest.isPresent()
                        && !artifact.getBaseVersion().equals(latest.get().toString())) {
                    LOG.infof("  => %s", latest.get().toString());
                    versionsToUpgrade.put(artifact.getGroupId() + ":" + artifact.getArtifactId(),
                            latest.get().toString());
                } else {
                    LOG.info("  => no change");
                }
            } catch (RepositoryException e) {
                LOG.errorf("Could not resolve %s", rangeArtifact.toString());
            }
        }
        return versionsToUpgrade;
    }

    private static Artifact newArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, split[2]);
    }

    private static Artifact newVersionRangeArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, "[" + split[2] + ",)");
    }

}
