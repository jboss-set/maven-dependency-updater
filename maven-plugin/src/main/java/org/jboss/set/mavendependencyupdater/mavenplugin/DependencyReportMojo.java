package org.jboss.set.mavendependencyupdater.mavenplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.TextReportProcessingStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

@Mojo(name = "report")
public class DependencyReportMojo extends AbstractUpdaterMojo {

    private static final String REPORT_FILE = "dependency-upgrades-report.txt";

    @Override
    protected void processComponentUpgrades(File pomFile, List<DependencyEvaluator.ComponentUpgrade> componentUpgrades)
            throws MojoExecutionException {
        getLog().info("Writing dependency upgrades report for project " + project.getName());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        TextReportProcessingStrategy strategy = new TextReportProcessingStrategy(configuration, pomFile, ps);
        strategy.process(componentUpgrades);

        File reportFile = new File(project.getBuild().getDirectory(), REPORT_FILE);
        try {
            baos.writeTo(new FileOutputStream(reportFile));
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write report file: " + reportFile.getPath(), e);
        }
    }
}
