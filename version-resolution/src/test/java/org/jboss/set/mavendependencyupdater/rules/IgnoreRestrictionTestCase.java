package org.jboss.set.mavendependencyupdater.rules;

import org.junit.Assert;
import org.junit.Test;

public class IgnoreRestrictionTestCase {

    @Test
    public void test() {
        String[] regexprs = {
                "\\.fuse-",
                "beta"
        };
        Restriction restriction = new IgnoreRestriction(regexprs);

        Assert.assertTrue(restriction.applies("1.2.3.Final", null));
        Assert.assertFalse(restriction.applies("1.2.3.fuse-1234-redhat-00001", null));
        Assert.assertFalse(restriction.applies("1.2.3.beta-01", null));
    }
}
