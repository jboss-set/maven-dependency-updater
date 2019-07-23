package org.jboss.set.mavendependencyupdater.dependencycollector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// TODO: remove
public class ProjectDependencyCollector {

    public static Set<Artifact> collectDependencies(File pomFile) throws IOException, XmlPullParserException {
        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        Dependency dependency = model.getDependencyManagement().getDependencies().get(0);
        return null;
    }
}
