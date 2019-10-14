package org.jboss.set.mavendependencyupdater;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.VersionRangeResult;

public interface AvailableVersionsResolver {

    /**
     * Resolves version range.
     *
     * @param artifact artifact with version range
     * @return list of available versions
     */
    VersionRangeResult resolveVersionRange(Artifact artifact) throws RepositoryException;

}
