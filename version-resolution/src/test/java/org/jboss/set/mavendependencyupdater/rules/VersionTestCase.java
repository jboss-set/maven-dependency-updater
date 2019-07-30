package org.jboss.set.mavendependencyupdater.rules;

import org.junit.Assert;
import org.junit.Test;

public class VersionTestCase {

    @Test
    public void testGetQualifier() {
        Assert.assertEquals("Final", Version.parse("1.2.3.Final").getQualifier());
        Assert.assertEquals("Final.redhat-00001", Version.parse("1.2.3.Final.redhat-00001").getQualifier());
        Assert.assertEquals("SP01.redhat-00001", Version.parse("1.SP01.redhat-00001").getQualifier());
        Assert.assertEquals("", Version.parse("1.2").getQualifier());
        Assert.assertEquals("v1.2.3", Version.parse("v1.2.3").getQualifier());
    }

    @Test
    public void testGetNumericalPart() {
        Assert.assertEquals("1.2.3", Version.parse("1.2.3.Final").getNumericalPart());
        Assert.assertEquals("1.2.3", Version.parse("1.2.3.Final.redhat-00001").getNumericalPart());
        Assert.assertEquals("1", Version.parse("1.SP01.redhat-00001").getNumericalPart());
        Assert.assertEquals("1.2", Version.parse("1.2").getNumericalPart());
        Assert.assertEquals("", Version.parse("v1.2.3").getNumericalPart());
    }

    @Test
    public void testGetNumericalSegments() {
        Assert.assertArrayEquals(new String[] {"1", "2", "3"}, Version.parse("1.2.3.Final").getNumericalSegments());
        Assert.assertArrayEquals(new String[] {"1", "2", "3"}, Version.parse("1.2.3").getNumericalSegments());
        Assert.assertArrayEquals(new String[0], Version.parse("v1.2.3").getNumericalSegments());
    }


    @Test
    public void testGetPrefix() {
        Assert.assertEquals("", Version.parse("1.2.3.Final.redhat-00001").getPrefix(0));
        Assert.assertEquals("1", Version.parse("1.2.3.Final.redhat-00001").getPrefix(1));
        Assert.assertEquals("1.2.3", Version.parse("1.2.3.Final.redhat-00001").getPrefix(3));
        Assert.assertEquals("1.2.3.Final.redhat", Version.parse("1.2.3.Final.redhat-00001").getPrefix(5));
        Assert.assertEquals("1.2.3.Final.redhat-00001", Version.parse("1.2.3.Final.redhat-00001").getPrefix(6));
        Assert.assertEquals("1.2.3.Final.redhat-00001", Version.parse("1.2.3.Final.redhat-00001").getPrefix(7));
    }
}
