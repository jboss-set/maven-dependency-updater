package org.jboss.set.mavendependencyupdater.projectparser;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
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
        Map<ProjectRef, Collection<ScopedArtifactRef>> projectsDeps =
                new PmeDependencyCollector(pomFile).getAllProjectsDependencies();
        Collection<ScopedArtifactRef> rootDeps = projectsDeps.values().iterator().next();

        Optional<ScopedArtifactRef> dep = findDependency(rootDeps, "pom-manipulation-common");
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
        Map<ProjectRef, Collection<ScopedArtifactRef>> projectsDeps = collector.getAllProjectsDependencies();
        Assert.assertEquals(3, projectsDeps.size());

        // root module
        Collection<ScopedArtifactRef> deps = collector.getRootProjectDependencies();
        Assert.assertEquals(2, deps.size());

        Optional<ScopedArtifactRef> dep = findDependency(deps, "pom-manipulation-common");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("3.7.1", dep.get().getVersionString());

        dep = findDependency(deps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.4", dep.get().getVersionString());

        // submodule1
        deps = projectsDeps.get(new SimpleProjectRef("org.jboss.set", "submodule1"));
        Assert.assertEquals(1, deps.size());

        dep = findDependency(deps, "commons-text");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.2", dep.get().getVersionString());

        // submodule2
        deps = projectsDeps.get(new SimpleProjectRef("org.jboss.set", "submodule2"));
        Assert.assertEquals(2, deps.size());

        dep = findDependency(deps, "commons-cli");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.3", dep.get().getVersionString());

        dep = findDependency(deps, "commons-text");
        Assert.assertTrue(dep.isPresent());
        Assert.assertEquals("1.2", dep.get().getVersionString());
    }

    private Optional<ScopedArtifactRef> findDependency(Collection<ScopedArtifactRef> dependencies, String artifactId) {
        return dependencies.stream().filter(d -> artifactId.equals(d.getArtifactId())).findFirst();
    }

    private File loadResource(String path) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(path);
        Assert.assertNotNull(resource);
        return new File(resource.toURI());
    }

}
