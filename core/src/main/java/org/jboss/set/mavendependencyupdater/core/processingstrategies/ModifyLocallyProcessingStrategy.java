package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;

import java.io.File;
import java.util.List;

/**
 * Performs all component upgrades locally. Doesn't commit and push anything.
 */
public class ModifyLocallyProcessingStrategy implements UpgradeProcessingStrategy {
    private File pomFile;

    public ModifyLocallyProcessingStrategy(File pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public boolean process(List<DependencyEvaluator.ComponentUpgrade> upgrades) {
        try {
            PomDependencyUpdater.upgradeDependencies(pomFile, upgrades);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upgrade dependencies in pom.xml", e);
        }
    }
}
