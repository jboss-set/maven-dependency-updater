package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.common.model.Project;

import java.time.LocalDateTime;

/**
 * Data bean wrapping component upgrade information.
 */
public class ComponentUpgrade {

    final private ArtifactRef artifact;
    final private String newVersion;
    final private String repository;
    final private LocalDateTime firstSeen;
    final private Project project;

    public ComponentUpgrade(ArtifactRef artifact, String newVersion, String repository, LocalDateTime firstSeen, Project project) {
        this.artifact = artifact;
        this.newVersion = newVersion;
        this.repository = repository;
        this.firstSeen = firstSeen;
        this.project = project;
    }

    public ComponentUpgrade(ArtifactRef artifact, String newVersion, String repository) {
        this(artifact, newVersion, repository, null, null);
    }

    public ArtifactRef getArtifact() {
        return artifact;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getRepository() {
        return repository;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public String toString() {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersionString()
                + " -> " + newVersion;
    }
}
