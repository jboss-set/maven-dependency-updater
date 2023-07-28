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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jboss.set.mavendependencyupdater.LocatedDependency.Type.DEPENDENCY;
import static org.jboss.set.mavendependencyupdater.LocatedDependency.Type.MANAGED_DEPENDENCY;
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
        List<ComponentUpgrade> upgrades = new ArrayList<>();
        upgrades.add(newUpgrade(newArtifactRef("commons-cli", "commons-cli", "1.4"), "1.4.redhat-00001"));
        upgrades.add(newUpgrade(newArtifactRef("org.jboss.logging", "jboss-logging", "3.4.0"), "3.4.0.redhat-00001"));
        upgrades.add(newUpgrade(newArtifactRef("junit", "junit", "4.12"), "4.13.redhat-00001"));
        upgrades.add(newUpgrade(newArtifactRef("org.apache.maven", "maven-core", "3.5.0"), "3.5.0.redhat-00001"));

        PomDependencyUpdater.upgradeDependencies(pomFile, upgrades);

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

    @Test
    public void testLocateDependency() throws IOException, XMLStreamException, XmlPullParserException {
        Optional<LocatedDependency> locatedDependency =
                PomDependencyUpdater.locateDependency(pomFile, newArtifactRef("commons-cli", "commons-cli", "1.4"));
        Assert.assertTrue(locatedDependency.isPresent());
        Assert.assertEquals(pomFile.toURI(), locatedDependency.get().getPom());
        Assert.assertEquals(MANAGED_DEPENDENCY, locatedDependency.get().getType());
        Assert.assertEquals("version.commons-cli", locatedDependency.get().getVersionProperty());

        locatedDependency =
                PomDependencyUpdater.locateDependency(pomFile, newArtifactRef("junit", "junit", "4.12"));
        Assert.assertTrue(locatedDependency.isPresent());
        Assert.assertEquals(pomFile.toURI(), locatedDependency.get().getPom());
        Assert.assertEquals(MANAGED_DEPENDENCY, locatedDependency.get().getType());
        Assert.assertNull(locatedDependency.get().getVersionProperty());

        locatedDependency =
                PomDependencyUpdater.locateDependency(pomFile, newArtifactRef("org.jboss.logging", "jboss-logging", "3.4.0.Final"));
        Assert.assertTrue(locatedDependency.isPresent());
        Assert.assertEquals(pomFile.toURI(), locatedDependency.get().getPom());
        Assert.assertEquals(DEPENDENCY, locatedDependency.get().getType());
        Assert.assertEquals("version.jboss-logging", locatedDependency.get().getVersionProperty());

        locatedDependency =
                PomDependencyUpdater.locateDependency(pomFile, newArtifactRef("org.apache.maven", "maven-core", "3.5.0"));
        Assert.assertTrue(locatedDependency.isPresent());
        Assert.assertEquals(pomFile.toURI(), locatedDependency.get().getPom());
        Assert.assertEquals(DEPENDENCY, locatedDependency.get().getType());
        Assert.assertNull(locatedDependency.get().getVersionProperty());
    }

    @Test
    public void testFollowCircularProperties() {
        File pomFile = new File("pom.xml");
        Model model = new Model();
        model.setPomFile(pomFile);
        model.getProperties().put("prop0", "${prop1}");
        model.getProperties().put("prop1", "${prop2}");
        model.getProperties().put("prop2", "${prop3}");
        model.getProperties().put("prop3", "${prop1}");
        Assert.assertEquals(new LocatedProperty(pomFile.toURI(), "prop0"), PomDependencyUpdater.followTransitiveProperties("prop0", model));
    }

    @Test
    public void testFollowProperties() {
        File pomFile = new File("pom.xml");
        Model model = new Model();
        model.setPomFile(pomFile);
        model.getProperties().put("prop1", "${prop2}");
        model.getProperties().put("prop2", "${prop3}");
        model.getProperties().put("prop3", "value");
        Assert.assertEquals(new LocatedProperty(pomFile.toURI(), "prop3"), PomDependencyUpdater.followTransitiveProperties("prop1", model));
    }

    private static ComponentUpgrade newUpgrade(ArtifactRef artifact, String newVersion) {
        return new ComponentUpgrade(artifact, newVersion, null, null, null);
    }
}
