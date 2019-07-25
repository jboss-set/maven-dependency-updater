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

}
