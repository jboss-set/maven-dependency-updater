package org.jboss.set.mavendependencyupdater;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
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
    public Map<ArtifactRef, String> getVersionsToUpgrade(Collection<ArtifactRef> dependencies) {
        Map<ArtifactRef, String> versionsToUpgrade = new HashMap<>();

        for (ArtifactRef dep : dependencies) {
            Artifact rangeArtifact = toVersionRangeArtifact(dep);

            if (dep.getVersionString().startsWith("$")) {
                LOG.infof("Skipping %s", dep);
                continue;
            }
            try {
                VersionStream stream =
                        configuration.getStreamFor(dep.getGroupId(), dep.getArtifactId(), MICRO);

                List<Version> versions = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                Optional<Version> latest = VersionUtils.findLatest(stream, dep.getVersionString(), versions);

                LOG.infof("Available versions for %s %s: %s", dep, stream, versions);
                if (latest.isPresent()
                        && !dep.getVersionString().equals(latest.get().toString())) {
                    LOG.infof("  => %s", latest.get().toString());
                    versionsToUpgrade.put(dep,
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

    private static Artifact toArtifact(ArtifactRef ref) {
        return new DefaultArtifact(ref.getGroupId(), ref.getArtifactId(), null, ref.getVersionString());
    }

    private static Artifact toVersionRangeArtifact(ArtifactRef ref) {
        return new DefaultArtifact(ref.getGroupId(), ref.getArtifactId(), null, "[" + ref.getVersionString() + ",)");
    }

}
