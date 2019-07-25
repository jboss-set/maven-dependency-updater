package org.jboss.set.mavendependencyupdater.common;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.common.ident.SimpleScopedArtifactRef;

public final class AtlasUtils {

    private AtlasUtils() {
    }

    public static ArtifactRef newArtifactRef(String g, String a, String v) {
        return new SimpleArtifactRef(g, a, v, null, null);
    }

    public static ScopedArtifactRef newScopedArtifactRef(String g, String a, String v, String scope) {
        return new SimpleScopedArtifactRef(g, a, v, null, null, scope);
    }
}
