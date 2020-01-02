package org.jboss.set.mavendependencyupdater.mavenplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.commonjava.maven.ext.common.ManipulationException;
import org.jboss.set.mavendependencyupdater.AvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DefaultAvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.TextReportProcessingStrategy;
import org.jboss.set.mavendependencyupdater.projectparser.PmeDependencyCollector;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Mojo(name = "report")
public class DependencyReportMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    @Inject
    private ProjectDependenciesResolver dependenciesResolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File pomFile = project.getModel().getPomFile();
        try {
            Collection<ScopedArtifactRef> rootProjectDependencies = new PmeDependencyCollector(pomFile).getRootProjectDependencies();
            Configuration configuration = new Configuration(null);
            TextReportProcessingStrategy strategy = new TextReportProcessingStrategy(configuration, pomFile, System.out);
            AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver(configuration);
            DependencyEvaluator evaluator = new DependencyEvaluator(configuration, availableVersionsResolver);
            List<DependencyEvaluator.ComponentUpgrade> componentUpgrades = evaluator.getVersionsToUpgrade(rootProjectDependencies);
            strategy.process(componentUpgrades);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't read configuration file.", e);
        } catch (ManipulationException e) {
            throw new MojoExecutionException("Problem when collecting project dependencies.", e);
        }
    }
}
