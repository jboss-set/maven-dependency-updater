package org.jboss.set.mavendependencyupdater.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MavenUtilsTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File pomFile;

    @Before
    public void setUp() throws IOException {
        pomFile = new File(tempDir.getRoot(), "pom.xml");
        URL resource = getClass().getClassLoader().getResource("pom.xml");
        Assert.assertNotNull(resource);
        Files.copy(resource.openStream(), pomFile.toPath());
    }

    @Test
    public void testUpgradePom() throws Exception {
        HashMap<String, String> deps = new HashMap<>();
        deps.put("commons-cli:commons-cli", "1.4.0-redhat-00001");
        deps.put("org.jboss.logging:jboss-logging", "3.4.0.Final-redhat-00001");
//        deps.put("junit:junit", "4.12-redhat-00001");

        MavenUtils.updateDependencyVersions(pomFile, deps);

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(pomFile));

        Assert.assertEquals("1.4.0-redhat-00001", model.getProperties().getProperty("version.commons-cli"));
        Assert.assertEquals("3.4.0.Final-redhat-00001", model.getProperties().getProperty("version.jboss-logging"));

        // TODO: not implemented
        /*Optional<Dependency> junitDep = model.getDependencyManagement().getDependencies().stream()
                .filter(d -> "junit".equals(d.getArtifactId())).findFirst();
        Assert.assertTrue(junitDep.isPresent());
        Assert.assertEquals("4.12-redhat-00001", junitDep.get().getVersion());*/
    }

}
