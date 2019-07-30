package org.jboss.set.mavendependencyupdater;

import org.apache.maven.model.Dependency;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

public class MavenUtils {

    public static boolean isProperty(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    public static String extractPropertyName(String value) {
        if (!isProperty(value)) {
            throw new IllegalArgumentException("Not a property: " + value);
        }
        return value.substring(2, value.length() - 1);
    }

    public static Optional<Dependency> findDependency(List<Dependency> dependencies, String artifactId) {
        return dependencies.stream().filter(d -> artifactId.equals(d.getArtifactId())).findFirst();
    }

    public static Optional<Dependency> findDependency(List<Dependency> dependencies, String groupId, String artifactId) {
        return dependencies.stream()
                .filter(d -> artifactId.equals(d.getArtifactId())
                        && groupId.equals(d.getGroupId()))
                .findFirst();
    }

}
