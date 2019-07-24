package org.jboss.set.mavendependencyupdater.utils;

import static org.jboss.set.mavendependencyupdater.VersionStream.MAJOR;
import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.VersionStream.QUALIFIER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;
import org.junit.Assert;
import org.junit.Test;

public class VersionUtilsTestCase {

    @Test
    public void testEqualMmm() {
        Assert.assertTrue(VersionUtils.equalMmm("1.0.0", "1.2.0", MAJOR));
        Assert.assertFalse(VersionUtils.equalMmm("1.0.0", "2.2.0", MAJOR));

        Assert.assertTrue(VersionUtils.equalMmm("1.0.1", "1.0.1", MICRO));
        Assert.assertTrue(VersionUtils.equalMmm("1.0.0", "1.0", MICRO));
        Assert.assertTrue(VersionUtils.equalMmm("1.0", "1.0.0.SP", MICRO));
        Assert.assertTrue(VersionUtils.equalMmm("1", "1.0.0.SP", MICRO));
        Assert.assertTrue(VersionUtils.equalMmm("1.0.0.1", "1.0.0.2", MICRO));
        Assert.assertTrue(VersionUtils.equalMmm("1.0.0.Beta1", "1.0.0.Beta2", MICRO));

        Assert.assertFalse(VersionUtils.equalMmm("1.0.0", "1.0.1", MICRO));
        Assert.assertFalse(VersionUtils.equalMmm("2.0.1", "1.0.1", MICRO));
        Assert.assertFalse(VersionUtils.equalMmm("1.0", "1.0.1", MICRO));
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

        Optional<Version> latest = VersionUtils.findLatest(MICRO, Collections.emptyList(), "1.1.1", availableVersions);
        Assert.assertEquals("1.1.4.redhat-00002", latest.get().toString());

        latest = VersionUtils.findLatest(QUALIFIER, Collections.emptyList(), "1.1.1", availableVersions);
        Assert.assertEquals("1.1.1.SP02", latest.get().toString());

        latest = VersionUtils.findLatest(QUALIFIER, Collections.emptyList(), "1.1.0", availableVersions);
        Assert.assertEquals("1.1.0", latest.get().toString());

        latest = VersionUtils.findLatest(QUALIFIER, Collections.emptyList(), "1.0.0", availableVersions);
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
        String original = "1.Final"; // doesn't matter

        availableVersions.add(scheme.parseVersion("1.Final"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("10.Final"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.1.Final", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.1.Final-redhat-00001"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.1.Final-redhat-00001", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.2"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.1.Final-redhat-00001", latest.get().toString());

        availableVersions.add(scheme.parseVersion("1.2.Final"));
        latest = VersionUtils.findLatest(versionStream, restrictions, original, availableVersions);
        Assert.assertEquals("1.2.Final", latest.get().toString());
    }
}
