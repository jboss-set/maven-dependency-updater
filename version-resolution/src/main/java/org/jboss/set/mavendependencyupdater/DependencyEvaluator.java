package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.utils.VersionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;

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
                LOG.warnf("Skipping '%s', should this be resolved?", dep);
                continue;
            }
            try {
                VersionStream stream =
                        configuration.getStreamFor(dep.getGroupId(), dep.getArtifactId(), MICRO);
                List<Restriction> restrictions =
                        configuration.getRestrictionsFor(dep.getGroupId(), dep.getArtifactId());

                List<Version> versions = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                Optional<Version> latest =
                        VersionUtils.findLatest(stream, restrictions, dep.getVersionString(), versions);

                LOG.debugf("Available versions for %s %s: %s", dep, stream, versions);
                if (latest.isPresent()
                        && !dep.getVersionString().equals(latest.get().toString())) {
                    LOG.infof("Upgrading %s to %s", dep, latest.get().toString());
                    versionsToUpgrade.put(dep,
                            latest.get().toString());
                } else {
                    LOG.debugf("  => no change");
                }
            } catch (RepositoryException e) {
                LOG.errorf("Could not resolve %s", rangeArtifact.toString());
            }
        }
        return versionsToUpgrade;
    }

    private static Artifact toVersionRangeArtifact(ArtifactRef ref) {
        return new DefaultArtifact(ref.getGroupId(), ref.getArtifactId(), null, "[" + ref.getVersionString() + ",)");
    }

}
