package org.jboss.set.mavendependencyupdater.mavenplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.ModifyLocallyProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.UpgradeProcessingStrategy;

import java.io.File;
import java.util.List;

@Mojo(name = "perform-upgrades")
public class PerformUpgradeMojo extends AbstractUpdaterMojo {

    @Override
    protected void processComponentUpgrades(File pomFile, List<ArtifactResult<ComponentUpgrade>> componentUpgrades)
            throws MojoExecutionException {
        getLog().info("Upgrading dependencies in project " + project.getName());

        UpgradeProcessingStrategy strategy = new ModifyLocallyProcessingStrategy(pomFile);
        try {
            strategy.process(componentUpgrades);
        } catch (Exception e) {
            throw new MojoExecutionException("Error when processing dependencies", e);
        }
    }
}
