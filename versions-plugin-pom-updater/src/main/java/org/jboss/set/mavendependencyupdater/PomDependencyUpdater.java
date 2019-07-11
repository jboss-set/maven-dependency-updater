package org.jboss.set.mavendependencyupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

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
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.common.MavenUtils;

public class PomDependencyUpdater {

    private static final Logger LOG = Logger.getLogger(PomDependencyUpdater.class);

    public static void upgradeDependencies(File pomFile, Map<String, String> dependencies)
            throws XMLStreamException, IOException, XmlPullParserException {
        StringBuilder content = PomHelper.readXmlFile(pomFile);
        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        ModifiedPomXMLEventReader pom = new ModifiedPomXMLEventReader(content, inputFactory);

        for (Map.Entry<String, String> entry: dependencies.entrySet()) {
            String[] ga = entry.getKey().split(":");
            if (ga.length != 2) {
                throw new IllegalArgumentException("Expected G:A");
            }


            // TODO: Temporary solution, old version should be handed over by a caller?
            // TODO: But still need to determine profile and whether the version is in property or in version tag.
            // TODO: Handle profiles.
            // TODO: Handle submodules.
            // TODO: Handle plugins?
            String oldVersion = null;
            String propertyName = null;
            Optional<Dependency> dependency = MavenUtils.findDependency(model.getDependencies(), ga[0], ga[1]);
            if (dependency.isPresent()) {
                String version = dependency.get().getVersion();
                if (!StringUtils.isEmpty(version)) {
                    if (MavenUtils.isProperty(version)) { // property
                        propertyName = MavenUtils.extractPropertyName(version);
                        oldVersion = model.getProperties().getProperty(propertyName);
                    } else { // version specified directly
                        oldVersion = version;
                    }
                }
            }
            if (oldVersion == null) {
                dependency = MavenUtils.findDependency(model.getDependencyManagement().getDependencies(), ga[0], ga[1]);
                if (dependency.isPresent()) {
                    String version = dependency.get().getVersion();
                    if (!StringUtils.isEmpty(version)) {
                        if (MavenUtils.isProperty(version)) { // property
                            propertyName = MavenUtils.extractPropertyName(version);
                            oldVersion = model.getProperties().getProperty(propertyName);
                        } else { // version specified directly
                            oldVersion = version;
                        }
                    }
                }
            }
            if (oldVersion == null) {
//                throw new IllegalArgumentException("Old version not determined for " + entry.getKey());
                LOG.errorf("Could not upgrade version of %s", entry.getKey());
                continue;
            }


            if (propertyName != null) { // version specified by a property
                PomHelper.setPropertyVersion(pom, null, propertyName, entry.getValue());
            } else { // version specified directly
                PomHelper.setDependencyVersion(pom, ga[0], ga[1], oldVersion, entry.getValue(), model);
            }
        }

        writeFile(pomFile, content);
    }

    private static void writeFile(File outFile, StringBuilder content)
            throws IOException {
        try (Writer writer = WriterFactory.newXmlWriter(outFile)) {
            IOUtil.copy(content.toString(), writer);
        }
    }
}
