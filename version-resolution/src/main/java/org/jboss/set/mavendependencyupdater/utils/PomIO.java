package org.jboss.set.mavendependencyupdater.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.BomExporter;

public class PomIO {

    private static Logger LOG = Logger.getLogger(PomIO.class);

    public static void updateDependencyVersions(File pomFile, Map<String, String> upgradedDependencies)
            throws IOException, XmlPullParserException {
        List<String> lines = Files.readAllLines(pomFile.toPath());

        LOG.debugf("Scanning managed dependencies in %s", pomFile.getPath());

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(pomFile));
        model.getDependencyManagement().getDependencies().forEach(dep -> {
            String version = dep.getVersion();
            LOG.debug("Found managed dependency " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + version);

            String ga = dep.getGroupId() + ":" + dep.getArtifactId();
            String newVersion = upgradedDependencies.get(ga);
            if (newVersion == null) {
                return;
            }

            if (StringUtils.isEmpty(version)) {
                throw new IllegalArgumentException("Expected version string in managed dependency.");
            } else if (version.startsWith("${") && version.endsWith("}")) {
                String propertyName = version.substring(2, version.length() - 1);
                String propertyValue = model.getProperties().getProperty(propertyName);

                LOG.infof("Upgrading dependency %s from %s to %s", ga, propertyValue, newVersion);
                LOG.debugf("Upgrading property %s from %s to %s", propertyName, propertyValue, newVersion);

                if (propertyName.equals("project.version")) {
                    throw new IllegalArgumentException("Can't upgrade property " + propertyName);
                }
                if (propertyValue == null) {
                    throw new IllegalArgumentException("Property value not found");
                }

                replaceProperty(lines, propertyName, newVersion);
            } else {
                // TODO
//                throw new NotImplementedException("Upgrading dependencies with version not defined by a property" +
//                        " not implemented.");
                LOG.errorf("Can't upgrade %s: Upgrading dependencies with version not defined by a property not implemented.", ga);
            }
        });

        Files.write(pomFile.toPath(), lines);
    }

    /**
     * Generates BOM file containing upgraded artifacts.
     *
     * @param bomFile target file
     * @param coordinates bom GAV
     * @param dependencies managed dependencies
     */
    public static void generateUpgradeBom(File bomFile, ArtifactRef coordinates, Map<String, String> dependencies)
            throws IOException {
        BomExporter exporter = new BomExporter(coordinates, dependencies);
        exporter.export(bomFile);
    }

    private static void replaceProperty(List<String> lines, String propertyName, String newValue) {
        String startTag = "<" + propertyName + ">";
        String endTag = "</" + propertyName + ">";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains(startTag) && line.contains(endTag)) {
                String indentation = line.substring(0, line.indexOf('<'));
                lines.set(i, indentation + startTag + newValue + endTag);
                return;
            }
        }
        throw new IllegalArgumentException("Property '" + propertyName + "' not found");
    }
}
