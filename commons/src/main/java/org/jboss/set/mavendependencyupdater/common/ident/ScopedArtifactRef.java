package org.jboss.set.mavendependencyupdater.common.ident;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

public interface ScopedArtifactRef extends ArtifactRef {
    String getScope();
}
