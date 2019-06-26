package org.jboss.set.mavenversionupdater;

import static org.jboss.set.mavenversionupdater.VersionComparison.ComparisonLevel.MAJOR;
import static org.jboss.set.mavenversionupdater.VersionComparison.ComparisonLevel.MICRO;
import static org.jboss.set.mavenversionupdater.VersionComparison.ComparisonLevel.MINOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.junit.Assert;
import org.junit.Test;

public class VersionComparisonTestCase {

    @Test
    public void testEqualMmm() {
        Assert.assertTrue(VersionComparison.equalMmm("1.0.0", "1.2.0", MAJOR));
        Assert.assertFalse(VersionComparison.equalMmm("1.0.0", "2.2.0", MAJOR));

        Assert.assertTrue(VersionComparison.equalMmm("1.0.1", "1.0.1", MICRO));
        Assert.assertTrue(VersionComparison.equalMmm("1.0.0", "1.0", MICRO));
        Assert.assertTrue(VersionComparison.equalMmm("1.0", "1.0.0.SP", MICRO));
        Assert.assertTrue(VersionComparison.equalMmm("1", "1.0.0.SP", MICRO));
        Assert.assertTrue(VersionComparison.equalMmm("1.0.0.1", "1.0.0.2", MICRO));
        Assert.assertTrue(VersionComparison.equalMmm("1.0.0.Beta1", "1.0.0.Beta2", MICRO));

        Assert.assertFalse(VersionComparison.equalMmm("1.0.0", "1.0.1", MICRO));
        Assert.assertFalse(VersionComparison.equalMmm("2.0.1", "1.0.1", MICRO));
        Assert.assertFalse(VersionComparison.equalMmm("1.0", "1.0.1", MICRO));
    }

    @Test
    public void testFindLatest() throws InvalidVersionSpecificationException {
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

        Optional<Version> latest = VersionComparison.findLatest(MINOR, "1.1.1", availableVersions);
        Assert.assertEquals("1.1.4.redhat-00002", latest.get().toString());

        latest = VersionComparison.findLatest(MICRO, "1.1.1", availableVersions);
        Assert.assertEquals("1.1.1.SP02", latest.get().toString());

        latest = VersionComparison.findLatest(MICRO, "1.1.0", availableVersions);
        Assert.assertEquals("1.1.0", latest.get().toString());

        latest = VersionComparison.findLatest(MICRO, "1.0.0", availableVersions);
        Assert.assertFalse(latest.isPresent());
    }
}
