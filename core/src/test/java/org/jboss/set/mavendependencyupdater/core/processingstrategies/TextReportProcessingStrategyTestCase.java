package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class TextReportProcessingStrategyTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private UpgradeProcessingStrategy strategy;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        URL pomResource = getClass().getClassLoader().getResource("pom.xml");
        Assert.assertNotNull(pomResource);
        File pomXml = new File(pomResource.toURI());

        URL configResource = getClass().getClassLoader().getResource("configuration.json");
        Assert.assertNotNull(configResource);
        File config = new File(configResource.toURI());

        outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        strategy = new TextReportProcessingStrategy(new Configuration(config), pomXml, printStream);
    }

    @Test
    public void test() throws Exception {
        ArrayList<ArtifactResult<ComponentUpgrade>> upgrades = new ArrayList<>();
        upgrades.add(ArtifactResult.of(new ComponentUpgrade(SimpleArtifactRef.parse(
                "org.jboss.logging:jboss-logging:3.4.0.Final"), "3.4.1.Final", "MRRC")));
        upgrades.add(ArtifactResult.of(new ComponentUpgrade(SimpleArtifactRef.parse(
                "org.jboss.logging:jboss-logging-annotations:3.4.0.Final"), "3.4.1.Final", "MRRC")));
        upgrades.add(ArtifactResult.of(new ComponentUpgrade(SimpleArtifactRef.parse(
                "io.undertow:undertow-core:1.0.0"), "1.0.1", "MRRC")));
        upgrades.add(ArtifactResult.of(new ComponentUpgrade(SimpleArtifactRef.parse(
                "io.undertow:undertow-jsp:1.0.0"), "1.0.1", "MRRC")));
        strategy.process(upgrades);

        String output = outputStream.toString();
        Assert.assertTrue(output.contains("* Central: https://repo1.maven.org/maven2/"));
        Assert.assertTrue(output.contains("org.jboss.logging:jboss-logging:3.4.0.Final -> 3.4.1.Final"));
        // annotations upgrade should be omitted since it uses the same version property
        Assert.assertFalse(output.contains("org.jboss.logging:jboss-logging-annotations:3.4.0.Final -> 3.4.1.Final"));
        Assert.assertTrue(output.contains("io.undertow:undertow-core:1.0.0 -> 1.0.1"));
        // undertow-jsp upgrade should be omitted since it uses the same transitive version property
        Assert.assertFalse(output.contains("io.undertow:undertow-jsp:1.0.0 -> 1.0.1"));
        Assert.assertTrue(output.contains("\n2 items"));
    }
}
