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

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private File dependenciesFile;
    private File configurationFile;
    private File bomFile;

    public static void main(String[] args) {
        try {
            System.exit(new Main().run(args));
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
        options.addOption(Option.builder("o")
                .longOpt("output")
                .required()
                .hasArgs()
                .numberOfArgs(1)
                .desc("Output BOM file")
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

        AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver();
        DependencyUpdater updater = new DependencyUpdater(dependenciesFile, configurationFile, availableVersionsResolver);
        updater.generateUpgradeBom(bomFile);

        return 0;
    }
}
