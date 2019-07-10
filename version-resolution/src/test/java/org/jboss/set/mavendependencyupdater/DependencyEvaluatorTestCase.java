package org.jboss.set.mavendependencyupdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DependencyEvaluatorTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private DependencyEvaluator updater;
    private URL dependenciesFile;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        URL configResource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(configResource);
        Configuration configuration = new Configuration(new File(configResource.toURI()));

        dependenciesFile = getClass().getClassLoader().getResource("dependencies.txt");
        Assert.assertNotNull(dependenciesFile);

        AvailableVersionsResolverMock resolver = new AvailableVersionsResolverMock();
        resolver.setResult("org.wildfly:wildfly-messaging",
                Arrays.asList("1.1.1", "1.1.2", "1.2.0")); // MINOR
        resolver.setResult("org.picketlink:picketlink-impl",
                Arrays.asList("1.1.1.SP01", "1.1.1.SP02", "1.1.2.SP01", "1.1.2.SP02")); // SP
        resolver.setResult("org.wildfly:wildfly-core",
                Arrays.asList("1.1.1", "1.1.2", "1.2.3")); // MICRO

        updater = new DependencyEvaluator(configuration, resolver);
    }

    @Test
    public void testGetVersionsToUpgrade() throws IOException, URISyntaxException {
        Map<String, String> versions = updater.getVersionsToUpgrade(
                Files.readAllLines(Paths.get(dependenciesFile.toURI())));

        Assert.assertEquals("1.2.0", versions.get("org.wildfly:wildfly-messaging"));
        Assert.assertEquals("1.1.1.SP02", versions.get("org.picketlink:picketlink-impl"));
        Assert.assertEquals("1.1.2", versions.get("org.wildfly:wildfly-core"));
    }
}
