package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newArtifactRef;

public class DependencyEvaluatorTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private DependencyEvaluator updater;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        URL configResource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(configResource);
        Configuration configuration = new Configuration(new File(configResource.toURI()));

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
    public void testGetVersionsToUpgrade() {
        ArrayList<ArtifactRef> artifactRefs = new ArrayList<>();

        ArtifactRef refMessaging, refCore, refPicketlink;

        artifactRefs.add(refMessaging = newArtifactRef("org.wildfly", "wildfly-messaging", "1.1.1"));
        artifactRefs.add(refPicketlink = newArtifactRef("org.picketlink", "picketlink-impl", "1.1.1.SP01"));
        artifactRefs.add(refCore = newArtifactRef("org.wildfly", "wildfly-core", "1.1.1"));

        Map<ArtifactRef, String> upgradedVersions = updater.getVersionsToUpgrade(artifactRefs);

        Assert.assertEquals("1.2.0", upgradedVersions.get(refMessaging));
        Assert.assertEquals("1.1.1.SP02", upgradedVersions.get(refPicketlink));
        Assert.assertEquals("1.1.2", upgradedVersions.get(refCore));
    }
}
