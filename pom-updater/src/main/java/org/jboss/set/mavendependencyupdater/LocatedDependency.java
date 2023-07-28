package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.net.URI;

public class LocatedDependency {

    private final ArtifactRef artifact;
    private final Type type;
    private final String versionProperty;
    private final URI pom;
    private final String profile;
    private final LocatedProperty locatedProperty;

    LocatedDependency(ArtifactRef artifact, Type type, String versionProperty, URI pom, String profile, LocatedProperty locatedProperty) {
        this.artifact = artifact;
        this.type = type;
        this.versionProperty = versionProperty;
        this.pom = pom;
        this.profile = profile;
        this.locatedProperty = locatedProperty;
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

    public LocatedProperty getLocatedProperty() {
        return locatedProperty;
    }

    enum Type {
        DEPENDENCY, MANAGED_DEPENDENCY
    }
}
