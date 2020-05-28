package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator.ComponentUpgrade;
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

/**
 * Prints upgradable dependencies report to stdout or to given file.
 * <p>
 * Non thread safe.
 */
public class TextReportProcessingStrategy implements UpgradeProcessingStrategy {

    protected static final Logger LOG = Logger.getLogger(TextReportProcessingStrategy.class);

    protected static final String PROJECT_URL = "https://github.com/jboss-set/maven-dependency-updater";
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss z yyyy-MM-dd");
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    final protected File pomFile;
    final protected Configuration configuration;
    final protected Set<ModifiedProperty> recordedUpdates = new HashSet<>();
    protected PrintStream outputStream;
    protected File outputFile;

    TextReportProcessingStrategy(Configuration configuration, File pomFile) {
        this.configuration = configuration;
        this.pomFile = pomFile;
    }

    public TextReportProcessingStrategy(Configuration configuration, File pomFile, String outputFileName) {
        this(configuration, pomFile);
        this.outputFile = new File(outputFileName);
    }

    public TextReportProcessingStrategy(Configuration configuration, File pomFile, PrintStream outputStream) {
        this(configuration, pomFile);
        this.outputStream = outputStream;
    }

    protected void initOutputStream() {
        try {
            if (this.outputStream == null) {
                if (this.outputFile == null) {
                    this.outputStream = System.out;
                } else {
                    this.outputStream = new PrintStream(this.outputFile);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can't create output stream", e);
        }
    }

    @Override
    public boolean process(List<ComponentUpgrade> upgrades) {
        try {
            if (upgrades.size() == 0) {
                LOG.info("No components to upgrade.");
                return true;
            }
            initOutputStream();

            List<ComponentUpgrade> sortedUpgrades =
                    upgrades.stream().sorted(new ComponentUpgradeComparator())
                            .collect(Collectors.toList());

            outputStream.println("Generated at " + DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
            outputStream.println();
            outputStream.println("Searched in following repositories:\n");
            for (Map.Entry<String, String> entry : configuration.getRepositories().entrySet()) {
                outputStream.println("* " + entry.getKey() + ": " + entry.getValue());
            }
            outputStream.println();
            outputStream.println("Possible upgrades:\n");

            int counter = 0;
            URI uri = pomFile.toURI();
            for (ComponentUpgrade upgrade : sortedUpgrades) {
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
                        outputStream.println(
                                String.format("%s:%s:%s -> %s (%s) - %s",
                                        artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(),
                                        newVersion, repoId,
                                        upgrade.getFirstSeen() == null ?
                                                "new" : "since " + upgrade.getFirstSeen().format(DATE_FORMATTER)
                                ));
                    }
                }
            }

            outputStream.println("\n" + counter + " items");
            outputStream.println("\nReport generated by Maven Dependency Updater");
            outputStream.println(PROJECT_URL);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Report generation failed", e);
        } finally {
            if (outputStream != null && outputStream != System.out) {
                outputStream.close();
            }
        }
    }

    /**
     * Identifies a property in a particular POM file and a profile.
     *
     * It's used to record properties that would be changed by a component upgrade, to detect that given property would
     * have already been changed by a previous component upgrade.
     */
    protected static class ModifiedProperty {

        final private URI pomUri;
        final private String profile;
        final private String propertyName;
        final private String newValue;

        ModifiedProperty(URI pomUri, String profile, String propertyName, String newValue) {
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

    /**
     * Comparator for sorting component upgrades. Sort primarily by first seen date, then alphabetically.
     */
    protected static class ComponentUpgradeComparator implements Comparator<ComponentUpgrade> {
        @Override
        public int compare(ComponentUpgrade o1, ComponentUpgrade o2) {
            if (o1.getFirstSeen() == null && o2.getFirstSeen() == null) {
                return o1.getArtifact().compareTo(o2.getArtifact());
            } else if (o1.getFirstSeen() == null) {
                return 1;
            } else if (o2.getFirstSeen() == null) {
                return -1;
            } else {
                int res = o1.getFirstSeen().toLocalDate().compareTo(o2.getFirstSeen().toLocalDate());
                if (res == 0) {
                    res = o1.getArtifact().toString().compareTo(o2.getArtifact().toString());
                }
                return res;
            }
        }
    }
}
