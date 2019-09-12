package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class PatchDigestRecorderTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File pomXml;

    @Before
    public void setUp() throws IOException {
        File repoDir = temporaryFolder.newFolder("repo");

        InputStream pomXmlResource = getClass().getClassLoader().getResourceAsStream("pom.xml");
        Assert.assertNotNull(pomXmlResource);
        pomXml = new File(repoDir, "pom.xml");
        Files.copy(pomXmlResource, pomXml.toPath());
    }

    @Test
    public void testRecordPatchDigest() throws IOException {
        PatchDigestRecorder digestRecorder = new PatchDigestRecorder();

        Pair<ArtifactRef, String> previous = digestRecorder.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a1:1"), "2");
        Assert.assertNull(previous);

        previous = digestRecorder.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a2:1"), "2");
        Assert.assertNotNull(previous);
        Assert.assertEquals("a1", previous.getLeft().getArtifactId());

        previous = digestRecorder.recordPatchDigest(pomXml, SimpleArtifactRef.parse("g:a2:1"), "2");
        Assert.assertNotNull(previous);
        Assert.assertEquals("a1", previous.getLeft().getArtifactId());
    }

}
