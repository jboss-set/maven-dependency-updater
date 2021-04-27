package org.jboss.set.mavendependencyupdater.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.commonjava.maven.ext.common.ManipulationException;
import org.jboss.set.mavendependencyupdater.AvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DefaultAvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClient;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClientFactory;
import org.jboss.set.mavendependencyupdater.projectparser.PmeDependencyCollector;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

public abstract class AbstractUpdaterMojo extends AbstractMojo {

    private static final String CONFIGURATION_FILE = "dependency-upgrades-config.json";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    protected Configuration configuration;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initConfig();

        File pomFile = project.getModel().getPomFile();
        try {
            LoggerClient loggerClient = null;
            if (configuration.getLogger().isSet()) {
                getLog().info("Logger service URI is " + configuration.getLogger().getUri());
                getLog().info("Logger project code is " + configuration.getLogger().getProjectCode());
                loggerClient = LoggerClientFactory.createClient(URI.create(configuration.getLogger().getUri()));
            } else {
                getLog().info("Logger service not configured.");
            }

            Collection<ScopedArtifactRef> rootProjectDependencies = new PmeDependencyCollector(pomFile).getRootProjectDependencies();
            AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver(configuration);
            DependencyEvaluator evaluator = new DependencyEvaluator(configuration, availableVersionsResolver, loggerClient);

            List<DependencyEvaluator.ComponentUpgrade> componentUpgrades = evaluator.getVersionsToUpgrade(rootProjectDependencies);

            if (componentUpgrades.size() > 0) {
                getLog().info("Found upgradeable dependencies for project " + project.getName() + ": ");
                for (DependencyEvaluator.ComponentUpgrade upgrade: componentUpgrades) {
                    getLog().info("  " + upgrade.toString());
                }
                processComponentUpgrades(pomFile, componentUpgrades);
            } else {
                getLog().info("No upgradeable dependencies found for project " + project.getName());
            }
        } catch (ManipulationException e) {
            throw new MojoExecutionException("Problem when collecting project dependencies.", e);
        }
    }

    protected abstract void processComponentUpgrades(File pomFile, List<DependencyEvaluator.ComponentUpgrade> componentUpgrades)
            throws MojoExecutionException;

    private void initConfig() throws MojoExecutionException {
        try {
            File executionProjectDirectory = project.getExecutionProject().getModel().getProjectDirectory();
            File configurationFile = new File(executionProjectDirectory, CONFIGURATION_FILE);
            if (configurationFile.exists()) {
                getLog().info("Using configuration file " + configurationFile.getPath());
                configuration = new Configuration(configurationFile);
            } else {
                getLog().info("No configuration file detected.");
                configuration = new Configuration(null);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't read dependency updater configuration", e);
        }
    }

}
