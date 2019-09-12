package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;
import org.jboss.set.mavendependencyupdater.git.GitRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextReportProcessingStrategy implements UpgradeProcessingStrategy {

    private static final Logger LOG = Logger.getLogger(TextReportProcessingStrategy.class);

    private File pomFile;
    private GitRepository gitRepository;
    private PatchDigestRecorder digestRecorder = new PatchDigestRecorder();

    public TextReportProcessingStrategy(File pomFile) {
        this.pomFile = pomFile;
        File gitDir = new File(pomFile.getParent(), ".git");
        try {
            this.gitRepository = new GitRepository(gitDir, null);
        } catch (IOException e) {
            throw new RuntimeException("Failure when reading git repository: " + gitDir, e);
        }
    }

    @Override
    public boolean process(Map<ArtifactRef, String> upgrades) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd");
        System.out.println("Generated at " + formatter.format(LocalDateTime.now()));
        System.out.println();

        List<Map.Entry<ArtifactRef, String>> sortedEntries = upgrades.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey)).collect(Collectors.toList());

        try {
            for (Map.Entry<ArtifactRef, String> entry : sortedEntries) {
                ArtifactRef artifact = entry.getKey();
                String newVersion = entry.getValue();
                PomDependencyUpdater.upgradeDependencies(pomFile, Collections.singletonMap(artifact, newVersion));

                Pair<ArtifactRef, String> previous = digestRecorder.recordPatchDigest(pomFile, artifact, newVersion);
                gitRepository.resetLocalChanges();

                if (previous == null) {
                    System.out.println(String.format("%s:%s:%s -> %s",
                            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(), newVersion));
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Report generation failed", e);
        } finally {
            try {
                gitRepository.resetLocalChanges();
            } catch (GitAPIException e) {
                LOG.error("Can't reset local changes", e);
            }
        }
    }
}
