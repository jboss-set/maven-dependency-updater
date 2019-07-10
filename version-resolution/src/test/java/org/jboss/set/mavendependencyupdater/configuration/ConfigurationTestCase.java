package org.jboss.set.mavendependencyupdater.configuration;

import static org.jboss.set.mavendependencyupdater.VersionStream.MICRO;
import static org.jboss.set.mavendependencyupdater.VersionStream.MINOR;
import static org.jboss.set.mavendependencyupdater.VersionStream.QUALIFIER;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTestCase {

    @Test
    public void test() throws Exception {
        URL resource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(resource);
        Configuration config = new Configuration(new File(resource.toURI()));

        // fully defined by "org.wildfly:wildfly-messaging"
        Assert.assertEquals(MINOR, config.getStreamFor("org.wildfly", "wildfly-messaging", null));
        // defined by wildcard "org.picketlink:*"
        Assert.assertEquals(QUALIFIER, config.getStreamFor("org.picketlink", "picketlink-config", null));
        // defined by wildcard "*:*"
        Assert.assertEquals(MICRO, config.getStreamFor("org.wildfly", "wildfly-core", null));
    }
}
