package org.jboss.set.mavendependencyupdater.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.AvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DefaultAvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.projectparser.PmeDependencyCollector;
import org.jboss.set.mavendependencyupdater.utils.PomIO;

public class Cli {

    private static final Logger LOG = Logger.getLogger(Cli.class);

    private File dependenciesFile;
    private Configuration configuration;
    private File bomFile;
    private File pomFile;

    public static void main(String[] args) {
        try {
            System.exit(new Cli().run(args));
        } catch (Exception e) {
            LOG.error(e);
            System.exit(1);
        }
    }

    private int run(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Print help").build());
        options.addOption(Option.builder("d")
                .longOpt("dependencies")
                .hasArgs()
                .numberOfArgs(1)
                .desc("Dependencies file (in GAV per line format)")
                .build());
        options.addOption(Option.builder("c")
                .longOpt("config")
                .required()
                .hasArgs()
                .numberOfArgs(1)
                .desc("Configuration JSON file")
                .build());
        options.addOption(Option.builder("b")
                .longOpt("bom")
                .hasArgs()
                .numberOfArgs(1)
                .desc("BOM file with upgraded dependencies to be generated")
                .build());
        options.addOption(Option.builder("f")
                .longOpt("file")
                .hasArgs()
                .numberOfArgs(1)
                .desc("POM file to be upgraded")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.debug("Caught problem parsing ", e);
            System.err.println(e.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("...", options);
            return 10;
        }

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("...", options);
            return 0;
        }
        if (cmd.hasOption('d')) {
            dependenciesFile = new File(cmd.getOptionValue('d'));
        }
        if (cmd.hasOption('c')) {
            File configurationFile = new File(cmd.getOptionValue('c'));
            configuration = new Configuration(configurationFile);
        }
        if (cmd.hasOption('b')) {
            bomFile = new File(cmd.getOptionValue('b'));
        }
        if (cmd.hasOption('f')) {
            pomFile = new File(cmd.getOptionValue('f'));
        }

        if (dependenciesFile == null && pomFile == null) {
            System.err.println("Either `dependencies` or `file` switch must be set.");
            return 1;
        }
        if (bomFile == null && pomFile == null) {
            System.err.println("Either `bom` or `file` switch must be set.");
            return 1;
        }

        AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver();
        DependencyEvaluator updater = new DependencyEvaluator(configuration, availableVersionsResolver);

        Collection<String> dependencies;
        if (dependenciesFile != null) {
            dependencies = Files.readAllLines(dependenciesFile.toPath());
        } else {
            Set<ArtifactRef> artifactRefs = new PmeDependencyCollector(pomFile).collectProjectDependencies();
            dependencies = artifactRefs.stream()
                    .map(ref -> String.format("%s:%s:%s", ref.getGroupId(), ref.getArtifactId(), ref.getVersionString()))
                    .collect(Collectors.toList());
        }

        Map<String, String> newVersions = updater.getVersionsToUpgrade(dependencies);

        if (bomFile != null) {
            PomIO.generateUpgradeBom(bomFile, configuration.getBomCoordinates(), newVersions);
        } else if (pomFile != null) {
            PomIO.updateDependencyVersions(pomFile, newVersions);
        }

        return 0;
    }
}
