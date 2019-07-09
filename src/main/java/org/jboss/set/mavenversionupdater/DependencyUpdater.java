package org.jboss.set.mavenversionupdater;

import static org.jboss.set.mavenversionupdater.VersionStream.MICRO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavenversionupdater.configuration.Configuration;
import org.jboss.set.mavenversionupdater.utils.PomIO;
import org.jboss.set.mavenversionupdater.utils.VersionUtils;

public class DependencyUpdater {

    private static final Logger LOG = Logger.getLogger(DependencyUpdater.class);

    private File dependenciesFile;
    private Configuration configuration;
    private AvailableVersionsResolver availableVersionsResolver;

    public DependencyUpdater(File dependenciesFile, File configurationFile,
                             AvailableVersionsResolver availableVersionsResolver) throws IOException {
        this.dependenciesFile = dependenciesFile;
        this.configuration = new Configuration(configurationFile);
        this.availableVersionsResolver = availableVersionsResolver;
    }

    public void upgradePom(File pomFile) throws IOException, XmlPullParserException {
        PomIO.updateDependencyVersions(pomFile, getVersionsToUpgrade());
    }

    /**
     * Generates BOM file containing upgraded artifacts.
     *
     * @param bomFile target file
     */
    public void generateUpgradeBom(File bomFile) throws IOException {
        BomExporter exporter = new BomExporter(configuration.getBomCoordinates(), getVersionsToUpgrade());
        exporter.export(bomFile);
    }

    /**
     * Determine which artifacts can be upgraded.
     *
     * @return returns map G:A => newVersion
     */
    public Map<String, String> getVersionsToUpgrade() throws IOException {
        List<String> dependencies = Files.readAllLines(dependenciesFile.toPath());
        Map<String, String> versionsToUpgrade = new HashMap<>();

        for (String gav : dependencies) {
            if (gav.isEmpty()) {
                continue;
            }

            Artifact artifact = newArtifact(gav);
            Artifact rangeArtifact = newVersionRangeArtifact(gav);

            if (artifact.getBaseVersion().startsWith("$")) {
                LOG.infof("Skipping %s", artifact);
                continue;
            }
            try {
                VersionStream stream =
                        configuration.getStreamFor(artifact.getGroupId(), artifact.getArtifactId(), MICRO);

                List<Version> versions = availableVersionsResolver.resolveVersionRange(rangeArtifact);
                Optional<Version> latest = VersionUtils.findLatest(stream, artifact.getBaseVersion(), versions);

                System.out.println(String.format("Available versions for %s %s: %s", gav, stream, versions));
                if (latest.isPresent()
                        && !artifact.getBaseVersion().equals(latest.get().toString())) {
                    System.out.println(String.format("  => %s", latest.get().toString()));
                    versionsToUpgrade.put(artifact.getGroupId() + ":" + artifact.getArtifactId(),
                            latest.get().toString());
                } else {
                    System.out.println("  => no change");
                }
            } catch (RepositoryException e) {
                LOG.errorf("Could not resolve %s", rangeArtifact.toString());
            }
        }
        return versionsToUpgrade;
    }

    private static Artifact newArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, split[2]);
    }

    private static Artifact newVersionRangeArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, "[" + split[2] + ",)");
    }

}
