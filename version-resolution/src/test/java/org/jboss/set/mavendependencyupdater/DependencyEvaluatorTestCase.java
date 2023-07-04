package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.common.ident.SimpleScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.loggerclient.ComponentUpgradeDTO;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClient;
import org.jboss.set.mavendependencyupdater.rules.NeverRestriction;
import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;
import org.jboss.set.mavendependencyupdater.rules.VersionStreamRestriction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.VersionStream.QUALIFIER;
import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newScopedArtifactRef;

public class DependencyEvaluatorTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private DependencyEvaluator evaluator;
    private LoggerClient loggerClientMock;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        URL configResource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(configResource);
        Configuration configuration = new Configuration(new File(configResource.toURI()));
        loggerClientMock = Mockito.mock(LoggerClient.class);

        AvailableVersionsResolverMock resolver = new AvailableVersionsResolverMock();
        resolver.setResult("org.wildfly:wildfly-messaging",
                Arrays.asList("1.1.1", "1.1.2", "1.2.0")); // MINOR
        resolver.setResult("org.picketlink:picketlink-impl",
                Arrays.asList("1.1.1.SP01", "1.1.1.SP02", "1.1.2.SP01", "1.1.2.SP02")); // SP
        resolver.setResult("org.wildfly:wildfly-core",
                Arrays.asList("10.0.0.Beta1", "10.0.0.Beta2", "10.0.1.Beta3")); // prefix "10.0.0" with qualifier "Beta\\d+"
        resolver.setResult("org.apache.cxf.xjc-utils:cxf-xjc-runtime",
                Arrays.asList("3.2.3.redhat-00002", "3.2.4.fuse-740019-redhat-00003", "3.3.0")); // fuse should be ignored
        resolver.setResult("junit:junit", Arrays.asList("4.8.1", "4.12"));

        evaluator = new DependencyEvaluator(configuration, resolver, loggerClientMock);
    }

    @Test
    public void testGetVersionsToUpgrade() {
        ArrayList<ScopedArtifactRef> artifactRefs = new ArrayList<>();

        artifactRefs.add(newScopedArtifactRef("org.wildfly", "wildfly-messaging", "1.1.1", "compile"));
        artifactRefs.add(newScopedArtifactRef("org.picketlink", "picketlink-impl", "1.1.1.SP01", "compile"));
        artifactRefs.add(newScopedArtifactRef("org.wildfly", "wildfly-core", "10.0.0.Beta1", "compile"));
        artifactRefs.add(newScopedArtifactRef("org.apache.cxf.xjc-utils", "cxf-xjc-runtime", "3.2.3.redhat-00002", "compile"));
        artifactRefs.add(newScopedArtifactRef("junit", "junit", "4.8", "test"));

        List<ArtifactResult<ComponentUpgrade>> componentUpgrades = evaluator.getVersionsToUpgrade(artifactRefs);

        Assert.assertEquals(4, componentUpgrades.size());
        Assert.assertEquals("1.2.0", componentUpgrades.get(0).getLatestConfigured().get().getNewVersion());
        Assert.assertEquals("1.1.1.SP02", componentUpgrades.get(1).getLatestConfigured().get().getNewVersion());
        Assert.assertEquals("10.0.0.Beta2", componentUpgrades.get(2).getLatestConfigured().get().getNewVersion());
        Assert.assertFalse(componentUpgrades.get(3).getLatestConfigured().isPresent());
    }


    @Test
    public void testFindLatestByStream() throws InvalidVersionSpecificationException {
        GenericVersionScheme scheme = new GenericVersionScheme();

        List<Version> availableVersions = new ArrayList<>();
        availableVersions.add(scheme.parseVersion("1.1.0"));
        availableVersions.add(scheme.parseVersion("1.1.1"));
        availableVersions.add(scheme.parseVersion("1.1.1.SP01"));
        availableVersions.add(scheme.parseVersion("1.1.1.SP02"));
        availableVersions.add(scheme.parseVersion("1.1.4.redhat-00002"));
        availableVersions.add(scheme.parseVersion("1.1.4.redhat-00001"));
        availableVersions.add(scheme.parseVersion("1.1.4"));
        availableVersions.add(scheme.parseVersion("1.1.3"));
        availableVersions.add(scheme.parseVersion("1.2.5"));

        List<Restriction> restrictionsMicro = Collections.singletonList(new VersionStreamRestriction(MICRO));
        List<Restriction> restrictionsQualifier = Collections.singletonList(new VersionStreamRestriction(QUALIFIER));

        ArtifactResult<Version> result = evaluator.findLatest(newDependency("1.1.1"), restrictionsMicro, availableVersions);
        Assert.assertTrue(result.getLatestConfigured().isPresent());
        Assert.assertEquals("1.1.4.redhat-00002", result.getLatestConfigured().get().toString());

        result = evaluator.findLatest(newDependency("1.1.1"), restrictionsQualifier, availableVersions);
        Assert.assertTrue(result.getLatestConfigured().isPresent());
        Assert.assertEquals("1.1.1.SP02", result.getLatestConfigured().get().toString());

        result = evaluator.findLatest(newDependency("1.1.0"), restrictionsQualifier, availableVersions);
        Assert.assertFalse(result.getLatestConfigured().isPresent());

        result = evaluator.findLatest(newDependency("1.0.0"), restrictionsQualifier, availableVersions);
        Assert.assertFalse(result.getLatestConfigured().isPresent());
    }

    @Test
    public void testFindLatestWithRestrictions() throws InvalidVersionSpecificationException {
        GenericVersionScheme scheme = new GenericVersionScheme();

        List<Restriction> restrictions = new ArrayList<>();
        restrictions.add(new VersionPrefixRestriction("1")); // version must start with "1."
        restrictions.add(new QualifierRestriction(new String[] { // Q must be ".Final" or ".Final-redhat-xxxxx"
                "Final",
                "Final-redhat-\\d+"
        }));

        List<Version> availableVersions = new ArrayList<>();
        ArtifactResult<Version> latest;

        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("test", "test", "1.Final", "jar", null, "compile");


        availableVersions.add(scheme.parseVersion("1.Final"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertFalse(latest.getLatestConfigured().isPresent());
        //Assert.assertEquals("1.Final", latest.getLatestConfigured().get().toString());

        availableVersions.add(scheme.parseVersion("10.Final"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertFalse(latest.getLatestConfigured().isPresent());
//        Assert.assertEquals("1.Final", latest.getLatestConfigured().get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertTrue(latest.getLatestConfigured().isPresent());
        Assert.assertEquals("1.1.Final", latest.getLatestConfigured().get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final-redhat-00001"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertTrue(latest.getLatestConfigured().isPresent());
        Assert.assertEquals("1.1.Final-redhat-00001", latest.getLatestConfigured().get().toString());

        availableVersions.add(scheme.parseVersion("1.2"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertTrue(latest.getLatestConfigured().isPresent());
        Assert.assertEquals("1.1.Final-redhat-00001", latest.getLatestConfigured().get().toString());

        availableVersions.add(scheme.parseVersion("1.2.Final"));
        latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertTrue(latest.getLatestConfigured().isPresent());
        Assert.assertEquals("1.2.Final", latest.getLatestConfigured().get().toString());
    }

    @Test
    public void testBlacklisted() throws InvalidVersionSpecificationException {
        GenericVersionScheme scheme = new GenericVersionScheme();
        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("org.jboss.test", "test", "1.0.0", "jar", null, "compile");

        List<Version> availableVersions = new ArrayList<>();
        availableVersions.add(scheme.parseVersion("1.0.0"));
        availableVersions.add(scheme.parseVersion("1.0.1"));

        ArtifactResult<Version> latest =
                evaluator.findLatest(dependency, Collections.singletonList(NeverRestriction.INSTANCE), availableVersions);
        Assert.assertFalse(latest.getLatestConfigured().isPresent());
    }

    @Test
    public void testRestrictionDoesntMatchCurrentVersion() throws InvalidVersionSpecificationException {
        GenericVersionScheme scheme = new GenericVersionScheme();

        List<Restriction> restrictions = Collections.singletonList(new VersionPrefixRestriction("1.1"));

        List<Version> availableVersions = new ArrayList<>();
        availableVersions.add(scheme.parseVersion("1.0.1"));
        availableVersions.add(scheme.parseVersion("1.1.1"));
        availableVersions.add(scheme.parseVersion("1.1.2"));

        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("test", "test", "1.0.0", "jar", null, "compile");


        ArtifactResult<Version> latest = evaluator.findLatest(dependency, restrictions, availableVersions);
        Assert.assertFalse(latest.getLatestConfigured().isPresent());
    }

    @Test
    public void testCombinationOfSPAndRedHatSuffix() throws Exception {
        GenericVersionScheme scheme = new GenericVersionScheme();

        List<Version> availableVersions = new ArrayList<>();
        availableVersions.add(scheme.parseVersion("1.3.16.SP1-redhat-6"));
        availableVersions.add(scheme.parseVersion("1.3.16.redhat-3"));
        availableVersions.add(scheme.parseVersion("1.3.16.SP1"));
        availableVersions.add(scheme.parseVersion("1.3.16"));

        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("test", "test", "1.3.16", "jar", null, "compile");


        ArtifactResult<Version> latest = evaluator.findLatest(dependency, Collections.emptyList(), availableVersions);
        Assert.assertTrue(latest.getLatestConfigured().isPresent());
        Assert.assertEquals("1.3.16.SP1-redhat-6", latest.getLatestConfigured().get().toString());
    }

    @Test
    public void testSendDetectedUpgradesToExternalService() {
        //noinspection unchecked
        ArgumentCaptor<List<ComponentUpgradeDTO>> listCaptor = ArgumentCaptor.forClass(List.class);
        evaluator.sendDetectedUpgradesToExternalService(createComponentUpgradeInstances(29));
        Mockito.verify(loggerClientMock, Mockito.times(1)).create(listCaptor.capture());
        Assert.assertEquals(29, listCaptor.getValue().size());

        //noinspection unchecked
        listCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.reset(loggerClientMock);
        evaluator.sendDetectedUpgradesToExternalService(createComponentUpgradeInstances(30));
        Mockito.verify(loggerClientMock, Mockito.times(1)).create(listCaptor.capture());
        Assert.assertEquals(30, listCaptor.getValue().size());

        //noinspection unchecked
        listCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.reset(loggerClientMock);
        evaluator.sendDetectedUpgradesToExternalService(createComponentUpgradeInstances(31));
        Mockito.verify(loggerClientMock, Mockito.times(2)).create(listCaptor.capture());
        List<List<ComponentUpgradeDTO>> sublists = listCaptor.getAllValues();
        Assert.assertEquals(30, sublists.get(0).size());
        Assert.assertEquals("29", sublists.get(0).get(29).newVersion);
        Assert.assertEquals(1, sublists.get(1).size());
        Assert.assertEquals("30", sublists.get(1).get(0).newVersion);
    }

    private ScopedArtifactRef newDependency(String version) {
        return new SimpleScopedArtifactRef("test", "test", version, "jar", null, "compile");
    }

    private List<ArtifactResult<ComponentUpgrade>> createComponentUpgradeInstances(int count) {
        List<ArtifactResult<ComponentUpgrade>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SimpleArtifactRef ref = new SimpleArtifactRef("group", "artifact", "1", null, "compile");
            list.add(ArtifactResult.of(new ComponentUpgrade(ref, String.valueOf(i), "repo")));
        }
        return list;
    }
}
