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
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;

public class PomDependencyUpdater {

    private static final Logger LOG = Logger.getLogger(PomDependencyUpdater.class);

    public static void upgradeDependencies(File pomFile, Map<ArtifactRef, DependencyEvaluator.ComponentUpgrade> dependencies)
            throws XMLStreamException, IOException, XmlPullParserException {
        StringBuilder content = PomHelper.readXmlFile(pomFile);
        Model model = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        ModifiedPomXMLEventReader pom = new ModifiedPomXMLEventReader(content, inputFactory);

        for (Map.Entry<ArtifactRef, DependencyEvaluator.ComponentUpgrade> entry: dependencies.entrySet()) {
            ArtifactRef ref = entry.getKey();

            // TODO: Need to determine profile.
            // TODO: Handle profiles.
            // TODO: Handle submodules.
            // TODO: Handle plugins?
            boolean found = false;
            String propertyName = null;
            Optional<Dependency> dependency =
                    MavenUtils.findDependency(model.getDependencies(), ref.getGroupId(), ref.getArtifactId());
            if (dependency.isPresent()) {
                String version = dependency.get().getVersion();
                if (!StringUtils.isEmpty(version)) {
                    if (MavenUtils.isProperty(version)) { // property
                        propertyName = MavenUtils.extractPropertyName(version);
                    }
                    found = true;
                }
            }
            if (!found) {
                dependency = MavenUtils.findDependency(model.getDependencyManagement().getDependencies(),
                        ref.getGroupId(), ref.getArtifactId());
                if (dependency.isPresent()) {
                    String version = dependency.get().getVersion();
                    if (!StringUtils.isEmpty(version)) {
                        if (MavenUtils.isProperty(version)) { // property
                            propertyName = MavenUtils.extractPropertyName(version);
                        }
                        found = true;
                    }
                }
            }
            if (!found) {
                LOG.errorf("Could not upgrade version of %s", ref);
                continue;
            }


            if (propertyName != null) { // version specified by a property
                PomHelper.setPropertyVersion(pom, null, propertyName, entry.getValue().getNewVersion());
            } else { // version specified directly
                PomHelper.setDependencyVersion(pom, ref.getGroupId(), ref.getArtifactId(), ref.getVersionString(),
                        entry.getValue().getNewVersion(), model);
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
