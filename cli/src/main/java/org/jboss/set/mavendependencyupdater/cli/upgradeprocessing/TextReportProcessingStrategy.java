package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.LocatedDependency;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TextReportProcessingStrategy implements UpgradeProcessingStrategy {

    private static final Logger LOG = Logger.getLogger(TextReportProcessingStrategy.class);

    private File pomFile;
    private Configuration configuration;
    private Set<ModifiedProperty> recordedUpdates = new HashSet<>();
    private PrintStream outputStream;

    public TextReportProcessingStrategy(Configuration configuration, File pomFile) {
        this(configuration, pomFile, new PrintStream(System.out));
    }

    public TextReportProcessingStrategy(Configuration configuration, File pomFile, String outputFileName)
            throws FileNotFoundException {
        this(configuration, pomFile, new PrintStream(outputFileName));
    }

    public TextReportProcessingStrategy(Configuration configuration, File pomFile, PrintStream outputStream) {
        this.configuration = configuration;
        this.pomFile = pomFile;
        this.outputStream = outputStream;
    }

    @Override
    public boolean process(List<DependencyEvaluator.ComponentUpgrade> upgrades) {
        try {
            if (upgrades.size() == 0) {
                LOG.info("No components to upgrade.");
                return true;
            }

            List<DependencyEvaluator.ComponentUpgrade> sortedUpgrades =
                    upgrades.stream().sorted(Comparator.comparing(DependencyEvaluator.ComponentUpgrade::getArtifact))
                            .collect(Collectors.toList());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss z yyyy-MM-dd");
            outputStream.println("Generated at " + formatter.format(ZonedDateTime.now()));
            outputStream.println();
            outputStream.println("Searched in following repositories:\n");
            for (Map.Entry<String, String> entry : configuration.getRepositories().entrySet()) {
                outputStream.println("* " + entry.getKey() + ": " + entry.getValue());
            }
            outputStream.println();
            outputStream.println("Possible upgrades:\n");

            int counter = 0;
            URI uri = pomFile.toURI();
            for (DependencyEvaluator.ComponentUpgrade upgrade : sortedUpgrades) {
                Optional<LocatedDependency> locatedDependencyOpt =
                        PomDependencyUpdater.locateDependency(pomFile, upgrade.getArtifact());
                ArtifactRef artifact = upgrade.getArtifact();
                String newVersion = upgrade.getNewVersion();
                String repoId = upgrade.getRepository();

                if (locatedDependencyOpt.isPresent()) {
                    LocatedDependency locatedDependency = locatedDependencyOpt.get();
                    boolean added = recordedUpdates.add(
                            new ModifiedProperty(uri, locatedDependency.getProfile(), locatedDependency.getVersionProperty(), newVersion));
                    if (added) {
                        counter++;
                        outputStream.println(String.format("%s:%s:%s -> %s (%s)",
                                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(), newVersion, repoId));
                    }
                }
                /*ArtifactRef artifact = upgrade.getArtifact();
                String newVersion = upgrade.getNewVersion();
                String repoId = upgrade.getRepository();
                Pair<ArtifactRef, String> previous = digestRecorder.recordPatchDigest(pomFile, artifact, newVersion);
                gitRepository.resetLocalChanges();

                if (previous == null) {
                    counter++;
                    outputStream.println(String.format("%s:%s:%s -> %s (%s)",
                            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(), newVersion, repoId));
                }*/
            }

            outputStream.println("\n" + counter + " items");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Report generation failed", e);
        } finally {
            if (outputStream != null && outputStream != System.out) {
                outputStream.close();
            }
        }
    }

    private static class ModifiedProperty {

        private URI pomUri;
        private String profile;
        private String propertyName;
        private String newValue;

        public ModifiedProperty(URI pomUri, String profile, String propertyName, String newValue) {
            this.pomUri = pomUri;
            this.profile = profile;
            this.propertyName = propertyName;
            this.newValue = newValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            ModifiedProperty that = (ModifiedProperty) o;

            return new EqualsBuilder()
                    .append(pomUri, that.pomUri)
                    .append(profile, that.profile)
                    .append(propertyName, that.propertyName)
                    .append(newValue, that.newValue)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(pomUri)
                    .append(profile)
                    .append(propertyName)
                    .append(newValue)
                    .toHashCode();
        }
    }
}
