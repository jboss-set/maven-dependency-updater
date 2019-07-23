package org.jboss.set.mavendependencyupdater.common;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;

public final class AtlasUtils {

    private AtlasUtils() {
    }

    public static ArtifactRef newArtifactRef(String g, String a, String v) {
        return new SimpleArtifactRef(g, a, v, null, null);
    }
}
