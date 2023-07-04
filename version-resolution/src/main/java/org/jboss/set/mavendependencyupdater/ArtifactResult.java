package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ArtifactResult<T> {

    public static <U> ArtifactResult<U> empty(ArtifactRef artifactRef) {
        return new ArtifactResult<>(artifactRef, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static <U> ArtifactResult<U> of(ArtifactRef artifactRef, U single) {
        return new ArtifactResult<>(artifactRef, Optional.of(single), Optional.empty(), Optional.empty());
    }

    public static ArtifactResult<ComponentUpgrade> of(ComponentUpgrade single) {
        return new ArtifactResult<>(single.getArtifact(), Optional.of(single), Optional.empty(), Optional.empty());
    }

    private final ArtifactRef artifactRef;
    private final Optional<T> latestConfigured;
    private final Optional<T> latestMinor;
    private final Optional<T> veryLatest;

    ArtifactResult(ArtifactRef artifactRef, Optional<T> latestConfigured, Optional<T> latestMinor, Optional<T> veryLatest) {
        this.artifactRef = artifactRef;
        this.latestConfigured = latestConfigured;
        this.latestMinor = latestMinor;
        this.veryLatest = veryLatest;
    }

    ArtifactResult(ArtifactRef artifactRef, T latestConfigured, T latestMinor, T veryLatest) {
        this.artifactRef = artifactRef;
        this.latestConfigured = Optional.ofNullable(latestConfigured);
        this.latestMinor = Optional.ofNullable(latestMinor);
        this.veryLatest = Optional.ofNullable(veryLatest);
    }

    public ArtifactRef getArtifactRef() {
        return artifactRef;
    }

    /**
     * @return The latest version that satisfies configured restrictions.
     */
    public Optional<T> getLatestConfigured() {
        return latestConfigured;
    }

    /**
     * @return The latest version inside the same major stream as the current version.
     */
    public Optional<T> getLatestMinor() {
        return latestMinor;
    }

    /**
     * @return The very latest version available, irrespective of stream.
     */
    public Optional<T> getVeryLatest() {
        return veryLatest;
    }

    public Optional<T> getAny() {
        return Optional.ofNullable(latestConfigured.orElse(latestMinor.orElse(veryLatest.orElse(null))));
    }

    public boolean anyPresent() {
        return latestConfigured.isPresent() || latestMinor.isPresent() || veryLatest.isPresent();
    }
}
