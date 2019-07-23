package org.jboss.set.mavendependencyupdater.projectparser;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class PmeDependencyCollectorTestCase {

    @Test
    public void testSimpleProject() throws ManipulationException, URISyntaxException {
        File pomFile = loadResource("simpleProject/pom.xml");
        Map<ProjectRef, Collection<ArtifactRef>> projectsDeps =
                new PmeDependencyCollector(pomFile).getAllProjectsDependencies();
        Collection<ArtifactRef> rootDeps = projectsDeps.values().iterator().next();

        Optional<ArtifactRef> dep = findDependency(rootDeps, "pom-manipulation-common");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("3.7.1", dep.get().getVersionString());

        dep = findDependency(rootDeps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.4", dep.get().getVersionString());
    }

    @Test
    public void testMultiModuleProject() throws ManipulationException, URISyntaxException {
        File pomFile = loadResource("multiModuleProject/pom.xml");
        PmeDependencyCollector collector = new PmeDependencyCollector(pomFile);
        Map<ProjectRef, Collection<ArtifactRef>> projectsDeps = collector.getAllProjectsDependencies();
        Assert.assertEquals(3, projectsDeps.size());

        // root module
        Collection<ArtifactRef> deps = collector.getRootProjectDependencies();

        Optional<ArtifactRef> dep = findDependency(deps, "pom-manipulation-common");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("3.7.1", dep.get().getVersionString());

        dep = findDependency(deps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.4", dep.get().getVersionString());

        // submodule1
        deps = projectsDeps.get(new SimpleProjectRef("org.jboss.set", "submodule1"));

        dep = findDependency(deps, "pom-manipulation-common");
        Assert.assertFalse(dep.isPresent());

        dep = findDependency(deps, "commons-cli");
        Assert.assertFalse(dep.isPresent());

        dep = findDependency(deps, "commons-text");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.2", dep.get().getVersionString());

        // submodule2
        deps = projectsDeps.get(new SimpleProjectRef("org.jboss.set", "submodule2"));

        dep = findDependency(deps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.3", dep.get().getVersionString());

        dep = findDependency(deps, "commons-text");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.2", dep.get().getVersionString());
    }

    private Optional<ArtifactRef> findDependency(Collection<ArtifactRef> dependencies, String artifactId) {
        return dependencies.stream().filter(d -> artifactId.equals(d.getArtifactId())).findFirst();
    }

    private File loadResource(String path) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(path);
        Assert.assertNotNull(resource);
        return new File(resource.toURI());
    }

}
