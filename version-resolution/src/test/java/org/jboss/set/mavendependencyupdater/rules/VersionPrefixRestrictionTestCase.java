package org.jboss.set.mavendependencyupdater.rules;

import org.junit.Assert;
import org.junit.Test;

public class VersionPrefixRestrictionTestCase {

    @Test
    public void testPrefixOnly() {
        VersionPrefixRestriction restriction = new VersionPrefixRestriction("1.2");

        Assert.assertTrue(restriction.applies("1.2"));
        Assert.assertTrue(restriction.applies("1.2.SP02"));
        Assert.assertTrue(restriction.applies("1.2-SP03"));
        Assert.assertTrue(restriction.applies("1.2.1"));
        Assert.assertTrue(restriction.applies("1.2.1.Final"));

        Assert.assertFalse(restriction.applies("1.21"));
        Assert.assertFalse(restriction.applies("1.3"));
    }

    @Test
    public void testSuffixRegex() {
        VersionPrefixRestriction restriction = new VersionPrefixRestriction("1.2", "(SP\\d+)?");

        Assert.assertTrue(restriction.applies("1.2"));
        Assert.assertTrue(restriction.applies("1.2.SP02"));
        Assert.assertTrue(restriction.applies("1.2-SP03"));

        Assert.assertFalse(restriction.applies("1.2.1"));
        Assert.assertFalse(restriction.applies("1.2.CR01"));
        Assert.assertFalse(restriction.applies("1.3"));


        restriction = new VersionPrefixRestriction("1.2", "(\\d+)?");

        Assert.assertTrue(restriction.applies("1.2.0"));
        Assert.assertTrue(restriction.applies("1.2.1"));

        Assert.assertFalse(restriction.applies("1.2.1.1"));
        Assert.assertFalse(restriction.applies("1.2.1.Final"));
        Assert.assertFalse(restriction.applies("1.2.CR01"));
        Assert.assertFalse(restriction.applies("1.3"));
    }
}
