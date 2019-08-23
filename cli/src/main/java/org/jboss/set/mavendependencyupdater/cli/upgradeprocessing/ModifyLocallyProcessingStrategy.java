package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;

import java.io.File;
import java.util.Map;

/**
 * Performs all component upgrades locally. Doesn't commit and push anything.
 */
public class ModifyLocallyProcessingStrategy implements UpgradeProcessingStrategy {
    private File pomFile;

    public ModifyLocallyProcessingStrategy(File pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public boolean process(Map<ArtifactRef, String> upgrades) {
        try {
            PomDependencyUpdater.upgradeDependencies(pomFile, upgrades);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upgrade dependencies in pom.xml", e);
        }
    }
}
