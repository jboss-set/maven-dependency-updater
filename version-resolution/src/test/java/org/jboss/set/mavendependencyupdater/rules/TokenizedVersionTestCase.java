package org.jboss.set.mavendependencyupdater.rules;

import org.junit.Assert;
import org.junit.Test;

public class TokenizedVersionTestCase {

    @Test
    public void testGetQualifier() {
        Assert.assertEquals("Final", TokenizedVersion.parse("1.2.3.Final").getQualifier());
        Assert.assertEquals("Final.redhat-00001", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getQualifier());
        Assert.assertEquals("SP01.redhat-00001", TokenizedVersion.parse("1.SP01.redhat-00001").getQualifier());
        Assert.assertEquals("", TokenizedVersion.parse("1.2").getQualifier());
        Assert.assertEquals("v1.2.3", TokenizedVersion.parse("v1.2.3").getQualifier());
    }

    @Test
    public void testGetNumericalPart() {
        Assert.assertEquals("1.2.3", TokenizedVersion.parse("1.2.3.Final").getNumericalPart());
        Assert.assertEquals("1.2.3", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getNumericalPart());
        Assert.assertEquals("1", TokenizedVersion.parse("1.SP01.redhat-00001").getNumericalPart());
        Assert.assertEquals("1.2", TokenizedVersion.parse("1.2").getNumericalPart());
        Assert.assertEquals("", TokenizedVersion.parse("v1.2.3").getNumericalPart());
    }

    @Test
    public void testGetNumericalSegments() {
        Assert.assertArrayEquals(new String[] {"1", "2", "3"}, TokenizedVersion.parse("1.2.3.Final").getNumericalSegments());
        Assert.assertArrayEquals(new String[] {"1", "2", "3"}, TokenizedVersion.parse("1.2.3").getNumericalSegments());
        Assert.assertArrayEquals(new String[0], TokenizedVersion.parse("v1.2.3").getNumericalSegments());
    }


    @Test
    public void testGetPrefix() {
        Assert.assertEquals("", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(0));
        Assert.assertEquals("1", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(1));
        Assert.assertEquals("1.2.3", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(3));
        Assert.assertEquals("1.2.3.Final.redhat", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(5));
        Assert.assertEquals("1.2.3.Final.redhat-00001", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(6));
        Assert.assertEquals("1.2.3.Final.redhat-00001", TokenizedVersion.parse("1.2.3.Final.redhat-00001").getPrefix(7));
    }

    @Test
    public void testGetBuildSuffixSegments() {
        Assert.assertArrayEquals(new String[] {"redhat", "00001"},
                TokenizedVersion.parse("1.2.3.Final.redhat-00001").getBuildSuffixSegments());
        Assert.assertArrayEquals(new String[] {"redhat", "00001"},
                TokenizedVersion.parse("1.2.3.SP01.redhat-00001").getBuildSuffixSegments());
        Assert.assertArrayEquals(new String[] {"redhat", "00001"},
                TokenizedVersion.parse("1.2.3.SP01.REDHAT-00001").getBuildSuffixSegments());
        Assert.assertArrayEquals(new String[] {}, TokenizedVersion.parse("1.2.3.SP01").getBuildSuffixSegments());
    }

    @Test
    public void testComparison() {
        TokenizedVersion v1 = TokenizedVersion.parse("1.2.3.SP01");
        TokenizedVersion v2 = TokenizedVersion.parse("1.2.3.redhat-00001");
        TokenizedVersion v3 = TokenizedVersion.parse("1.2.3.redhat-00002");
        TokenizedVersion v4 = TokenizedVersion.parse("1.2.3.jbossorg-00001");
        TokenizedVersion v5 = TokenizedVersion.parse("1.2.3.REDHAT-00001");

        Assert.assertTrue(v1.compareTo(v2) > 0);
        Assert.assertTrue(v2.compareTo(v3) < 0);
        Assert.assertTrue(v2.compareTo(v4) > 0);
        Assert.assertTrue(v1.compareTo(v5) > 0);
    }
}
