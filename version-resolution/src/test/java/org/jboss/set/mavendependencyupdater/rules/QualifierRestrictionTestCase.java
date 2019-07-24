package org.jboss.set.mavendependencyupdater.rules;

import org.junit.Assert;
import org.junit.Test;

public class QualifierRestrictionTestCase {

    @Test
    public void test() {
        String[] regexprs = {
                "Final",
                "Final-redhat-\\d+"
        };
        QualifierRestriction restriction = new QualifierRestriction(regexprs);

        Assert.assertTrue(restriction.applies("1.2.3.Final"));
        Assert.assertTrue(restriction.applies("1.Final"));
        Assert.assertTrue(restriction.applies("1.Final-redhat-00001"));

        Assert.assertFalse(restriction.applies("1.Final-jboss-00001"));
        Assert.assertFalse(restriction.applies("1.F"));
        Assert.assertFalse(restriction.applies("1.2"));
    }

    @Test
    public void testEmptyQualifier() {
        String[] regexprs = {
                "",
                "Final"
        };
        QualifierRestriction restriction = new QualifierRestriction(regexprs);

        Assert.assertTrue(restriction.applies("1.2.3.Final"));
        Assert.assertTrue(restriction.applies("1.2"));

        Assert.assertFalse(restriction.applies("1.Final-redhat-00001"));
        Assert.assertFalse(restriction.applies("1.F"));
    }
}
