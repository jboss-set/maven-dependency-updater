package org.jboss.set.mavendependencyupdater.cli;

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
import org.jboss.set.mavendependencyupdater.BomExporter;
import org.jboss.set.mavendependencyupdater.DefaultAvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.configuration.ConfigurationGenerator;
import org.jboss.set.mavendependencyupdater.projectparser.PmeDependencyCollector;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Cli {

    private static final Logger LOG = Logger.getLogger(Cli.class);

    private static final String ALIGN = "align";
    private static final String GENERATE_CONFIG = "generate-config";

    private Options options;
    private CommandLineParser parser = new DefaultParser();

    public static void main(String[] args) {
        try {
            System.exit(new Cli().run(args));
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public Cli() {
        options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Print help").build());
        options.addOption(Option.builder("c")
                .longOpt("config")
                .required()
                .hasArgs()
                .numberOfArgs(1)
                .desc("Configuration JSON file")
                .build());
        options.addOption(Option.builder("f")
                .longOpt("file")
                .required()
                .hasArgs()
                .numberOfArgs(1)
                .desc("POM file")
                .build());
    }

    private int run(String[] args) throws Exception {
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.debug("Caught problem parsing CLI arguments", e);
            System.err.println(e.getMessage());

            printHelp();
            return 10;
        }

        if (cmd.hasOption('h')) {
            printHelp();
            return 0;
        }

        String[] arguments = cmd.getArgs();
        if (arguments.length != 1) {
            System.err.println("Single action argument expected.");
            printHelp();
            return 10;
        }

        File configurationFile = new File(cmd.getOptionValue('c'));
        File pomFile = new File(cmd.getOptionValue('f'));


        Collection<ScopedArtifactRef> rootProjectDependencies =
                new PmeDependencyCollector(pomFile).getRootProjectDependencies();

        if (ALIGN.equals(arguments[0])) {
            Configuration configuration = new Configuration(configurationFile);
            AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver();
            DependencyEvaluator updater = new DependencyEvaluator(configuration, availableVersionsResolver);
            Map<ArtifactRef, String> newVersions = updater.getVersionsToUpgrade(rootProjectDependencies);
            PomDependencyUpdater.upgradeDependencies(pomFile, newVersions);
        } else if (GENERATE_CONFIG.equals(arguments[0])) {
            new ConfigurationGenerator().generateDefautlConfig(configurationFile, rootProjectDependencies);
        } else {
            System.err.println("Unknown action: " + arguments[0]);
            printHelp();
            return 10;
        }

        return 0;
    }

    private static void generateUpgradeBom(File bomFile, ArtifactRef coordinates, Map<ArtifactRef, String> dependencies)
            throws IOException {
        BomExporter exporter = new BomExporter(coordinates, dependencies);
        exporter.export(bomFile);
    }

    private void printHelp() {
        String header = "\nCommand is one of:\n\n" +
                "  align              Align dependencies in specified POM file\n" +
                "  generate-config    Generate basic dependency alignment configuration for\n" +
                "                     specified POM\n" +
                "\n";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <path/to/updater.jar> <command> <args>", header, options, "");
    }

}
