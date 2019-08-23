package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SeparatePRsProcessingStrategyTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testRecordPatchDigest() throws IOException {
        File pomXml = temporaryFolder.newFile("pom.xml");
        Files.write(pomXml.toPath(), "some content".getBytes());

        File config = temporaryFolder.newFile("config.json");
        Files.write(config.toPath(), "{}".getBytes());

        SeparatePRsProcessingStrategy strategy = new SeparatePRsProcessingStrategy(new Configuration(config), pomXml);

        Pair<ArtifactRef, String> previous = strategy.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a1:1"), "2");
        Assert.assertNull(previous);

        previous = strategy.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a2:1"), "2");
        Assert.assertNotNull(previous);
        Assert.assertEquals("a1", previous.getLeft().getArtifactId());

        previous = strategy.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a2:1"), "2");
        Assert.assertNotNull(previous);
        Assert.assertEquals("a1", previous.getLeft().getArtifactId());
    }
}
