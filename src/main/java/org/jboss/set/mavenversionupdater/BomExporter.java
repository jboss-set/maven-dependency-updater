package org.jboss.set.mavenversionupdater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

public class BomExporter {

    private ArtifactRef coords;
    private Map<String, String> dependencies;

    public BomExporter(ArtifactRef coords, Map<String, String> dependencies) {
        this.coords = coords;
        this.dependencies = dependencies;
    }

    public void export(File bomFile) throws IOException {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(coords.getGroupId());
        model.setArtifactId(coords.getArtifactId());
        model.setVersion(coords.getVersionString());
        model.setPackaging("pom");

        model.setDependencyManagement(new DependencyManagement());

        for (Map.Entry<String, String> entry: dependencies.entrySet()) {
            model.getDependencyManagement().addDependency(newDependency(entry.getKey(), entry.getValue()));
        }

        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileOutputStream(bomFile), model);
    }

    static Dependency newDependency(String ga, String version) {
        String[] split = ga.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid GA: " + ga);
        }

        Dependency dep = new Dependency();
        dep.setGroupId(split[0]);
        dep.setArtifactId(split[1]);
        dep.setVersion(version);

        return dep;
    }
}
