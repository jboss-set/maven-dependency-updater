package org.jboss.set.mavendependencyupdater.configuration;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.VersionStream.MINOR;
import static org.jboss.set.mavendependencyupdater.VersionStream.QUALIFIER;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTestCase {

    private Configuration config;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        URL resource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(resource);
        config = new Configuration(new File(resource.toURI()));
    }

    @Test
    public void testGitHubConfig() {
        Assert.assertEquals("TomasHofman/wildfly", config.getGitHub().getOriginRepository());
        Assert.assertEquals("wildfly/wildfly", config.getGitHub().getUpstreamRepository());
        Assert.assertEquals("joe", config.getGitHub().getLogin());
        Assert.assertEquals("1234abcd", config.getGitHub().getAccessToken());
    }

    @Test
    public void testGitConfig() {
        Assert.assertEquals("origin", config.getGit().getRemote());
        Assert.assertEquals("master", config.getGit().getBaseBranch());
    }

    @Test
    public void testIgnoreScopes() {
        Assert.assertTrue(config.getIgnoreScopes().contains("test"));
    }

    @Test
    public void testStreams() {
        // fully defined by "org.wildfly:wildfly-messaging"
        Assert.assertEquals(MINOR, config.getStreamFor("org.wildfly", "wildfly-messaging", null));
        // defined by wildcard "org.picketlink:*"
        Assert.assertEquals(QUALIFIER, config.getStreamFor("org.picketlink", "picketlink-config", null));
        // defined by wildcard "*:*"
        Assert.assertEquals(MICRO, config.getStreamFor("org.wildfly", "wildfly-core", null));
    }

    @Test
    public void testRestrictions() {
        List<Restriction> restrictions = config.getRestrictionsFor("org.picketlink", "picketlink-config");
        Assert.assertEquals(1, restrictions.size());
        Assert.assertTrue(restrictions.get(0) instanceof QualifierRestriction);
        Assert.assertTrue(restrictions.get(0).applies("1.Final"));
        Assert.assertTrue(restrictions.get(0).applies("1.SP02"));
        Assert.assertFalse(restrictions.get(0).applies("1.Beta1"));

        restrictions = config.getRestrictionsFor("org.wildfly", "wildfly-core");
        Assert.assertEquals(2, restrictions.size());
        Assert.assertTrue(restrictions.get(0) instanceof VersionPrefixRestriction);
        Assert.assertTrue(restrictions.get(0).applies("10.0.0"));
        Assert.assertTrue(restrictions.get(0).applies("10.0.0.1"));
        Assert.assertFalse(restrictions.get(0).applies("10.0.1"));
        Assert.assertTrue(restrictions.get(1) instanceof QualifierRestriction);
        Assert.assertTrue(restrictions.get(1).applies("1.Beta1"));
        Assert.assertFalse(restrictions.get(1).applies("1.Final"));
    }
}
