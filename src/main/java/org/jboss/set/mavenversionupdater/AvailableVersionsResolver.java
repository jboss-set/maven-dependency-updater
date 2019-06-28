package org.jboss.set.mavenversionupdater;

import java.util.List;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

public interface AvailableVersionsResolver {

    /**
     * Resolves version range.
     *
     * @param artifact artifact with version range
     * @return list of available versions
     */
    List<Version> resolveVersionRange(Artifact artifact) throws RepositoryException;

}
