package org.jboss.set.mavendependencyupdater.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
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
    private static final String CHECK_CONFIG = "check-config";
    private static final String[] COMMANDS = {ALIGN, GENERATE_CONFIG, CHECK_CONFIG};

    private static final String PREFIX_DOESNT_MATCH_MSG = "Dependency %s doesn't match prefix '%s'";

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

    private Cli() {
        options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Print help").build());
        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArgs()
                .numberOfArgs(1)
                .desc("Configuration JSON file")
                .build());
        options.addOption(Option.builder("f")
                .longOpt("file")
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
        if (!ArrayUtils.contains(COMMANDS, arguments[0])) {
            System.err.println("Unknown command: " + arguments[0]);
            printHelp();
            return 10;
        }

        if (!cmd.hasOption('c')) {
            System.err.println("Missing option 'c'");
            printHelp();
            return 10;
        }
        File configurationFile = new File(cmd.getOptionValue('c'));
        if (!cmd.hasOption('f')) {
            System.err.println("Missing option 'f'");
            printHelp();
            return 10;
        }
        File pomFile = new File(cmd.getOptionValue('f'));


        Collection<ScopedArtifactRef> rootProjectDependencies =
                new PmeDependencyCollector(pomFile).getRootProjectDependencies();

        if (ALIGN.equals(arguments[0])) {
            Configuration configuration = new Configuration(configurationFile);
            AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver();
            DependencyEvaluator evaluator = new DependencyEvaluator(configuration, availableVersionsResolver);
            Map<ArtifactRef, String> newVersions = evaluator.getVersionsToUpgrade(rootProjectDependencies);
            PomDependencyUpdater.upgradeDependencies(pomFile, newVersions);
        } else if (GENERATE_CONFIG.equals(arguments[0])) {
            new ConfigurationGenerator().generateDefautlConfig(configurationFile, rootProjectDependencies);
        } else if (CHECK_CONFIG.equals(arguments[0])) {
            Configuration configuration = new Configuration(configurationFile);
            Collection<Pair<ScopedArtifactRef, String>> outOfDate =
                    configuration.findOutOfDateRestrictions(rootProjectDependencies);
            outOfDate.forEach(p ->
                    System.err.println(String.format(PREFIX_DOESNT_MATCH_MSG, p.getLeft(), p.getRight()))
            );
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
        String header = "\nCommands:\n" +
                "  align              Aligns dependencies in given POM file\n" +
                "  generate-config    Generates somewhat sane base for dependency alignment\n" +
                "                     configuration for given project\n" +
                "  check-config       Checks if configuration is up-to-date\n" +
                "                     (project dependencies match configured version\n" +
                "                     prefixes)\n" +
                "\n" +
                "Parameters:\n";
        //      |<--- Line height of 74 chars                                          --->|

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <path/to/cli.jar> <command> <params>", header, options, "");
    }

}
