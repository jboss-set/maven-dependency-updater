package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
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

import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newScopedArtifactRef;

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
                Arrays.asList("10.0.0.Beta1", "10.0.0.Beta2", "10.0.1.Beta3")); // prefix "10.0.0" with qualifier "Beta\\d+"
        resolver.setResult("junit:junit", Arrays.asList("4.8.1", "4.12"));

        updater = new DependencyEvaluator(configuration, resolver);
    }

    @Test
    public void testGetVersionsToUpgrade() {
        ArrayList<ScopedArtifactRef> artifactRefs = new ArrayList<>();

        ScopedArtifactRef refMessaging, refCore, refPicketlink, refJunit;

        artifactRefs.add(refMessaging = newScopedArtifactRef("org.wildfly", "wildfly-messaging", "1.1.1", "compile"));
        artifactRefs.add(refPicketlink = newScopedArtifactRef("org.picketlink", "picketlink-impl", "1.1.1.SP01", "compile"));
        artifactRefs.add(refCore = newScopedArtifactRef("org.wildfly", "wildfly-core", "10.0.0.Beta1", "compile"));
        artifactRefs.add(refJunit = newScopedArtifactRef("junit", "junit", "4.8", "test"));

        Map<ArtifactRef, String> upgradedVersions = updater.getVersionsToUpgrade(artifactRefs);

        Assert.assertEquals("1.2.0", upgradedVersions.get(refMessaging));
        Assert.assertEquals("1.1.1.SP02", upgradedVersions.get(refPicketlink));
        Assert.assertEquals("10.0.0.Beta2", upgradedVersions.get(refCore));
        Assert.assertNull(upgradedVersions.get(refJunit)); // ignored scope
    }
}
