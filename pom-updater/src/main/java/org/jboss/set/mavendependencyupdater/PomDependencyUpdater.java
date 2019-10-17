package org.jboss.set.mavendependencyupdater;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.stax2.XMLInputFactory2;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.jboss.set.mavendependencyupdater.LocatedDependency.Type.DEPENDENCY;
import static org.jboss.set.mavendependencyupdater.LocatedDependency.Type.MANAGED_DEPENDENCY;

// TODO: Handle profiles.
// TODO: Handle submodules.
// TODO: Handle plugins.
public class PomDependencyUpdater {

    private static final Logger LOG = Logger.getLogger(PomDependencyUpdater.class);

    public static void upgradeDependencies(File pomFile, List<DependencyEvaluator.ComponentUpgrade> componentUpgrades)
            throws XMLStreamException, IOException, XmlPullParserException {
        StringBuilder content = PomHelper.readXmlFile(pomFile);
        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        ModifiedPomXMLEventReader pom = new ModifiedPomXMLEventReader(content, inputFactory);

        for (DependencyEvaluator.ComponentUpgrade componentUpgrade : componentUpgrades) {
            ArtifactRef artifact = componentUpgrade.getArtifact();
            Optional<LocatedDependency> locatedDependencyOpt = locateDependency(pomFile, artifact);
            if (!locatedDependencyOpt.isPresent()) {
                LOG.errorf("Dependency element for %s was not found.", artifact);
                continue;
            }

            LocatedDependency locatedDependency = locatedDependencyOpt.get();
            if (locatedDependency.getVersionProperty() != null) {
                PomHelper.setPropertyVersion(pom, locatedDependency.getProfile(),
                        locatedDependency.getVersionProperty(), componentUpgrade.getNewVersion());
            } else {
                PomHelper.setDependencyVersion(pom, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(),
                        componentUpgrade.getNewVersion(), model);
            }
        }

        writeFile(pomFile, content);
    }

    /**
     * Locates how a dependency dependency is defined, i.e. looks for dependency element for given artifact which has
     * version defined.
     *
     * @param pomFile pom.xml file to search
     * @param artifact artifact to look for
     * @return LocatedDependency instance with information about the dependency
     */
    public static Optional<LocatedDependency> locateDependency(File pomFile, ArtifactRef artifact)
            throws IOException, XmlPullParserException {
        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        // function that tries to locate a dependency in given list of dependencies
        BiFunction<LocatedDependency.Type, List<Dependency>, Optional<LocatedDependency>> dependencyLocationFn = (type, dependencies) -> {
            Optional<Dependency> dependency =
                    MavenUtils.findDependency(dependencies, artifact.getGroupId(), artifact.getArtifactId());
            if (dependency.isPresent()) {
                String version = dependency.get().getVersion();
                if (!StringUtils.isEmpty(version)) {
                    if (MavenUtils.isProperty(version)) {
                        String propertyName = MavenUtils.extractPropertyName(version);
                        propertyName = followTransitiveProperties(propertyName, model);
                        return Optional.of(new LocatedDependency(artifact, type, propertyName, pomFile.toURI(), null));
                    } else {
                        return Optional.of(new LocatedDependency(artifact, type, null, pomFile.toURI(), null));
                    }
                }
            }
            return Optional.empty();
        };

        // search in regular dependencies
        Optional<LocatedDependency> locatedDependencyOptional =
                dependencyLocationFn.apply(DEPENDENCY, model.getDependencies());
        if (!locatedDependencyOptional.isPresent()) {
            // search in managed dependencies
            locatedDependencyOptional =
                    dependencyLocationFn.apply(MANAGED_DEPENDENCY, model.getDependencyManagement().getDependencies());
        }

        return locatedDependencyOptional;
    }

    private static void writeFile(File outFile, StringBuilder content)
            throws IOException {
        try (Writer writer = WriterFactory.newXmlWriter(outFile)) {
            IOUtil.copy(content.toString(), writer);
        }
    }

    private static String followTransitiveProperties(String propertyName, Model model) {
        String value = model.getProperties().getProperty(propertyName);
        if (MavenUtils.isProperty(value)) {
            return followTransitiveProperties(MavenUtils.extractPropertyName(value), model);
        } else {
            return propertyName;
        }
    }
}
