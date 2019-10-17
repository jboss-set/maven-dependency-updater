package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.net.URI;

public class LocatedDependency {

    private ArtifactRef artifact;
    private Type type;
    private String versionProperty;
    private URI pom;
    private String profile;

    LocatedDependency(ArtifactRef artifact, Type type, String versionProperty, URI pom, String profile) {
        this.artifact = artifact;
        this.type = type;
        this.versionProperty = versionProperty;
        this.pom = pom;
        this.profile = profile;
    }

    public ArtifactRef getArtifact() {
        return artifact;
    }

    public Type getType() {
        return type;
    }

    public String getVersionProperty() {
        return versionProperty;
    }

    public URI getPom() {
        return pom;
    }

    public String getProfile() {
        return profile;
    }

    enum Type {
        DEPENDENCY, MANAGED_DEPENDENCY
    }
}
