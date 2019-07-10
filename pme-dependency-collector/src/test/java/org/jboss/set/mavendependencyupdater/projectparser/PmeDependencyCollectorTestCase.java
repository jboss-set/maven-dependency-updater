package org.jboss.set.mavendependencyupdater.projectparser;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PmeDependencyCollectorTestCase {

    private File pomFile;

    @Before
    public void setUp() throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("pom.xml");
        Assert.assertNotNull(resource);
        pomFile = new File(resource.toURI());
    }

    @Test
    public void testCollectDependencies() throws ManipulationException {
        Set<ArtifactRef> deps = new PmeDependencyCollector(pomFile).collectProjectDependencies();

        Optional<ArtifactRef> dep = findDependency(deps, "pom-manipulation-common");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("3.7.1", dep.get().getVersionString());

        dep = findDependency(deps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.4", dep.get().getVersionString());
    }

    private Optional<ArtifactRef> findDependency(Set<ArtifactRef> dependencies, String artifactId) {
        return dependencies.stream().filter(d -> artifactId.equals(d.getArtifactId())).findFirst();
    }
}
