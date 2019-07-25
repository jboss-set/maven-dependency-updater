package org.jboss.set.mavendependencyupdater.common.ident;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public class SimpleScopedArtifactRef extends SimpleArtifactRef implements ScopedArtifactRef {

    private String scope;

    public SimpleScopedArtifactRef(String groupId, String artifactId, String versionSpec, String type, String classifier, String scope)
            throws InvalidVersionSpecificationException {
        super(groupId, artifactId, versionSpec, type, classifier);
        this.scope = scope;
    }

    public <T extends ArtifactRef> SimpleScopedArtifactRef(ArtifactRef ref, String scope) {
        super(ref);
        this.scope = scope;
    }

    @Override
    public String getScope() {
        return scope;
    }
}
