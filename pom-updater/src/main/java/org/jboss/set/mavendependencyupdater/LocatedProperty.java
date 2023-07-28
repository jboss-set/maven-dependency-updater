package org.jboss.set.mavendependencyupdater;

import java.net.URI;
import java.util.Objects;

public class LocatedProperty {
    private final URI pom;
    private final String name;

    public LocatedProperty(URI pom, String name) {
        this.pom = pom;
        this.name = name;
    }

    public URI getPom() {
        return pom;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocatedProperty that = (LocatedProperty) o;
        return Objects.equals(pom, that.pom) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pom, name);
    }

    @Override
    public String toString() {
        return "LocatedProperty{" +
                "pom=" + pom +
                ", name='" + name + '\'' +
                '}';
    }
}
