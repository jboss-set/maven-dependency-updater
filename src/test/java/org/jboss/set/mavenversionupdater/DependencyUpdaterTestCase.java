package org.jboss.set.mavenversionupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DependencyUpdaterTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private DependencyUpdater updater;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        URL alignmentFile = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(alignmentFile);
        URL dependenciesFile = getClass().getClassLoader().getResource("dependencies.txt");
        Assert.assertNotNull(dependenciesFile);

        AvailableVersionsResolverMock resolver = new AvailableVersionsResolverMock();
        resolver.setResult("org.wildfly:wildfly-messaging",
                Arrays.asList("1.1.1", "1.1.2", "1.2.0")); // MINOR
        resolver.setResult("org.picketlink:picketlink-impl",
                Arrays.asList("1.1.1.SP01", "1.1.1.SP02", "1.1.2.SP01", "1.1.2.SP02")); // SP
        resolver.setResult("org.wildfly:wildfly-core",
                Arrays.asList("1.1.1", "1.1.2", "1.2.3")); // MICRO

        updater = new DependencyUpdater(new File(dependenciesFile.toURI()), new File(alignmentFile.toURI()), resolver);
    }

    @Test
    public void testGetVersionsToUpgrade() throws IOException {
        Map<String, String> versions = updater.getVersionsToUpgrade();

        Assert.assertEquals("1.2.0", versions.get("org.wildfly:wildfly-messaging"));
        Assert.assertEquals("1.1.1.SP02", versions.get("org.picketlink:picketlink-impl"));
        Assert.assertEquals("1.1.2", versions.get("org.wildfly:wildfly-core"));
    }

    @Test
    public void testGenerateUpgradeBom() throws IOException, XmlPullParserException {
        File bomFile = new File(tempDir.getRoot(), "pom.xml");
        updater.generateUpgradeBom(bomFile);

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
