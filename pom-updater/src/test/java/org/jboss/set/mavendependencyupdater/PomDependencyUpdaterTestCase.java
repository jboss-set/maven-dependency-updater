package org.jboss.set.mavendependencyupdater;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;

import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newArtifactRef;

public class PomDependencyUpdaterTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File pomFile;

    @Before
    public void setUp() throws IOException {
        pomFile = new File(tempDir.getRoot(), "pom.xml");

        URL resource = getClass().getClassLoader().getResource("pom.xml");
        Assert.assertNotNull(resource);

        Files.copy(resource.openStream(), pomFile.toPath());
    }

    @Test
    public void testUpgradeDependencies() throws IOException, XMLStreamException, XmlPullParserException {
        HashMap<ArtifactRef, String> deps = new HashMap<>();
        deps.put(newArtifactRef("commons-cli", "commons-cli", "1.4"), "1.4.redhat-00001");
        deps.put(newArtifactRef("org.jboss.logging", "jboss-logging", "3.4.0"), "3.4.0.redhat-00001");
        deps.put(newArtifactRef("junit", "junit", "4.12"), "4.13.redhat-00001");
        deps.put(newArtifactRef("org.apache.maven", "maven-core", "3.5.0"), "3.5.0.redhat-00001");

        PomDependencyUpdater.upgradeDependencies(pomFile, deps);

        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        // managed dependency, version specified by a property
        Optional<Dependency> dependency =
                MavenUtils.findDependency(model.getDependencyManagement().getDependencies(), "commons-cli");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertEquals("${version.commons-cli}", dependency.get().getVersion());
        Assert.assertEquals("1.4.redhat-00001", model.getProperties().getProperty("version.commons-cli"));
        // verify that version in <dependencies> section remains empty
        dependency = MavenUtils.findDependency(model.getDependencies(), "commons-cli");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertTrue(StringUtils.isEmpty(dependency.get().getVersion()));

        // managed dependency, version specified directly
        dependency = MavenUtils.findDependency(model.getDependencyManagement().getDependencies(), "junit");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertEquals("4.13.redhat-00001", dependency.get().getVersion());
        // verify that version in <dependencies> section remains empty
        dependency = MavenUtils.findDependency(model.getDependencies(), "junit");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertTrue(StringUtils.isEmpty(dependency.get().getVersion()));

        // non-managed dependency, version specified by a property
        dependency = MavenUtils.findDependency(model.getDependencies(), "jboss-logging");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertEquals("${version.jboss-logging}", dependency.get().getVersion());
        Assert.assertEquals("3.4.0.redhat-00001", model.getProperties().getProperty("version.jboss-logging"));

        // non-managed dependency, version specified directly
        dependency = MavenUtils.findDependency(model.getDependencies(), "maven-core");
        Assert.assertTrue(dependency.isPresent());
        Assert.assertEquals("3.5.0.redhat-00001", dependency.get().getVersion());
    }
}
