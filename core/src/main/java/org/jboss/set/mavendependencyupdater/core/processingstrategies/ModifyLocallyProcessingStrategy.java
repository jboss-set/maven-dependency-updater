package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Performs all component upgrades locally. Doesn't commit and push anything.
 */
public class ModifyLocallyProcessingStrategy implements UpgradeProcessingStrategy {
    private File pomFile;

    public ModifyLocallyProcessingStrategy(File pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public boolean process(List<ArtifactResult<ComponentUpgrade>> scopedUpdates) {
        try {
            List<ComponentUpgrade> updates = scopedUpdates.stream().map(ArtifactResult::getLatestConfigured)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            PomDependencyUpdater.upgradeDependencies(pomFile, updates);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upgrade dependencies in pom.xml", e);
        }
    }
}
