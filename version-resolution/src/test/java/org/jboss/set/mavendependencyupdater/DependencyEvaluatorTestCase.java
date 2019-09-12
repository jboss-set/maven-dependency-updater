package org.jboss.set.mavendependencyupdater;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.common.ident.SimpleScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.rules.NeverRestriction;
import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.VersionStream.QUALIFIER;
import static org.jboss.set.mavendependencyupdater.common.AtlasUtils.newScopedArtifactRef;

public class DependencyEvaluatorTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private DependencyEvaluator evaluator;

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

        evaluator = new DependencyEvaluator(configuration, resolver);
    }

    @Test
    public void testGetVersionsToUpgrade() {
        ArrayList<ScopedArtifactRef> artifactRefs = new ArrayList<>();

        ScopedArtifactRef refMessaging, refCore, refPicketlink, refJunit;

        artifactRefs.add(refMessaging = newScopedArtifactRef("org.wildfly", "wildfly-messaging", "1.1.1", "compile"));
        artifactRefs.add(refPicketlink = newScopedArtifactRef("org.picketlink", "picketlink-impl", "1.1.1.SP01", "compile"));
        artifactRefs.add(refCore = newScopedArtifactRef("org.wildfly", "wildfly-core", "10.0.0.Beta1", "compile"));
        artifactRefs.add(refJunit = newScopedArtifactRef("junit", "junit", "4.8", "test"));

        Map<ArtifactRef, String> upgradedVersions = evaluator.getVersionsToUpgrade(artifactRefs);

        Assert.assertEquals("1.2.0", upgradedVersions.get(refMessaging));
        Assert.assertEquals("1.1.1.SP02", upgradedVersions.get(refPicketlink));
        Assert.assertEquals("10.0.0.Beta2", upgradedVersions.get(refCore));
        Assert.assertNull(upgradedVersions.get(refJunit)); // ignored scope
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

        Optional<Version> latest = evaluator.findLatest(newDependency("1.1.1"), MICRO, Collections.emptyList(), availableVersions);
        Assert.assertEquals("1.1.4.redhat-00002", latest.get().toString());

        latest = evaluator.findLatest(newDependency("1.1.1"), QUALIFIER, Collections.emptyList(), availableVersions);
        Assert.assertEquals("1.1.1.SP02", latest.get().toString());

        latest = evaluator.findLatest(newDependency("1.1.0"), QUALIFIER, Collections.emptyList(), availableVersions);
        Assert.assertEquals("1.1.0", latest.get().toString());

        latest = evaluator.findLatest(newDependency("1.0.0"), QUALIFIER, Collections.emptyList(), availableVersions);
        Assert.assertFalse(latest.isPresent());
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
        Optional<Version> latest;
        VersionStream versionStream = MICRO; // will be ignored, because VersionPrefixRestriction has precedence

        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("test", "test", "1.Final", "jar", null, "compile");


        availableVersions.add(scheme.parseVersion("1.Final"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("10.Final"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final-redhat-00001"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.1.Final-redhat-00001", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.2"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.1.Final-redhat-00001", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.2.Final"));
        latest = evaluator.findLatest(dependency, versionStream, restrictions, availableVersions);
        Assert.assertEquals("1.2.Final", latest.get().toString());
    }

    @Test
    public void testBlacklisted() throws InvalidVersionSpecificationException {
        GenericVersionScheme scheme = new GenericVersionScheme();
        SimpleScopedArtifactRef dependency =
                new SimpleScopedArtifactRef("org.jboss.test", "test", "1.0.0", "jar", null, "compile");

        List<Version> availableVersions = new ArrayList<>();
        availableVersions.add(scheme.parseVersion("1.0.0"));
        availableVersions.add(scheme.parseVersion("1.0.1"));

        Optional<Version> latest =
                evaluator.findLatest(dependency, MICRO, Collections.singletonList(NeverRestriction.INSTANCE), availableVersions);
        Assert.assertFalse(latest.isPresent());
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


        Optional<Version> latest = evaluator.findLatest(dependency, null, restrictions, availableVersions);
        Assert.assertFalse(latest.isPresent());
    }

    private ScopedArtifactRef newDependency(String version) {
        return new SimpleScopedArtifactRef("test", "test", version, "jar", null, "compile");
    }
}
