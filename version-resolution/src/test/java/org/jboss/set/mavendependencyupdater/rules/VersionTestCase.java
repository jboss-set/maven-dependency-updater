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
}
