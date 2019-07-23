package org.jboss.set.mavendependencyupdater;

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
    private Map<ArtifactRef, String> dependencies;

    public BomExporter(ArtifactRef coords, Map<ArtifactRef, String> dependencies) {
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

        for (Map.Entry<ArtifactRef, String> entry: dependencies.entrySet()) {
            model.getDependencyManagement().addDependency(newDependency(entry.getKey(), entry.getValue()));
        }

        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileOutputStream(bomFile), model);
    }

    static Dependency newDependency(ArtifactRef ref, String version) {
        Dependency dep = new Dependency();
        dep.setGroupId(ref.getGroupId());
        dep.setArtifactId(ref.getArtifactId());
        dep.setVersion(version);
        return dep;
    }
}
