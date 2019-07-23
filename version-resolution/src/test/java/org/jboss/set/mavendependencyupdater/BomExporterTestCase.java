package org.jboss.set.mavendependencyupdater;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newArtifactRef;

public class BomExporterTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testGenerateUpgradeBom() throws IOException, XmlPullParserException {
        File bomFile = new File(tempDir.getRoot(), "pom.xml");

        HashMap<ArtifactRef, String> deps = new HashMap<>();
        deps.put(newArtifactRef("org.wildfly", "wildfly-messaging", "1.1.1"), "1.2.0");
        deps.put(newArtifactRef("org.picketlink", "picketlink-impl", "1.1.1.SP01"), "1.1.1.SP02");
        deps.put(newArtifactRef("org.wildfly", "wildfly-core", "1.1.1"), "1.1.2");

        new BomExporter(SimpleArtifactRef.parse("test:test:0.1"), deps).export(bomFile);

        Assert.assertTrue(bomFile.exists());

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(bomFile));
        Assert.assertEquals("test", model.getGroupId());
        Assert.assertEquals("test", model.getArtifactId());
        Assert.assertEquals("0.1", model.getVersion());

        Assert.assertNotNull(model.getDependencyManagement());
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        Assert.assertTrue(dependencyExists(dependencies, "org.wildfly", "wildfly-messaging", "1.2.0"));
        Assert.assertTrue(dependencyExists(dependencies, "org.picketlink", "picketlink-impl", "1.1.1.SP02"));
        Assert.assertTrue(dependencyExists(dependencies, "org.wildfly", "wildfly-core", "1.1.2"));
    }

    private boolean dependencyExists(List<Dependency> dependencies, String g, String a, String v) {
        for (Dependency dep: dependencies) {
            if (dep.getGroupId().equals(g) && dep.getArtifactId().equals(a) && dep.getVersion().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
