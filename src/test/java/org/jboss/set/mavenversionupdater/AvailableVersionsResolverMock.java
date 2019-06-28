package org.jboss.set.mavenversionupdater;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

public class AvailableVersionsResolverMock implements AvailableVersionsResolver {

    private Map<String, List<Version>> results = new HashMap<>();
    private VersionScheme versionScheme = new GenericVersionScheme();

    public void setResult(String ga, List<String> versions) {
        List<Version> converted = versions.stream()
                .map(ThrowingFunction.wrapper(s -> versionScheme.parseVersion(s)))
                .collect(Collectors.toList());
        results.put(ga, converted);
    }

    @Override
    public List<Version> resolveVersionRange(Artifact artifact) throws RepositoryException {
        List<Version> versions = results.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
        if (versions != null) {
            return versions;
        }
        return Collections.emptyList();
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;

        static <T, R> Function<T, R> wrapper(ThrowingFunction<T, R, Exception> function) {
            return t -> {
                try {
                    return function.apply(t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}
