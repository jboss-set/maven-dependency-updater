package org.jboss.set.mavenversionupdater;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.logging.Logger;

public class Cli {

    private static final Logger LOG = Logger.getLogger(Cli.class);

    private File dependenciesFile;
    private File configurationFile;
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
                .required()
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
                .desc("Generate BOM file with upgraded dependencies, argument is a file path")
                .build());
        options.addOption(Option.builder("f")
                .longOpt("file")
                .hasArgs()
                .numberOfArgs(1)
                .desc("pom.xml file that will be upgraded")
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
            System.exit(0);
        }
        if (cmd.hasOption('d')) {
            dependenciesFile = new File(cmd.getOptionValue('d'));
        }
        if (cmd.hasOption('c')) {
            configurationFile = new File(cmd.getOptionValue('c'));
        }
        if (cmd.hasOption('o')) {
            bomFile = new File(cmd.getOptionValue('o'));
        }
        if (cmd.hasOption('f')) {
            pomFile = new File(cmd.getOptionValue('f'));
        }

        AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver();
        DependencyUpdater updater = new DependencyUpdater(dependenciesFile, configurationFile, availableVersionsResolver);

        if (bomFile != null) {
            updater.generateUpgradeBom(bomFile);
        }
        if (pomFile != null) {
            updater.upgradePom(pomFile);
        }

        return 0;
    }
}
