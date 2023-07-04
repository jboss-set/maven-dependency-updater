package org.jboss.set.mavendependencyupdater.cli;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.AvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.DefaultAvailableVersionsResolver;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.configuration.ConfigurationGenerator;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.EmailReportProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.HtmlReportProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.ModifyLocallyProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.SeparatePRsProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.TextReportProcessingStrategy;
import org.jboss.set.mavendependencyupdater.core.processingstrategies.UpgradeProcessingStrategy;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClient;
import org.jboss.set.mavendependencyupdater.loggerclient.LoggerClientFactory;
import org.jboss.set.mavendependencyupdater.projectparser.PmeDependencyCollector;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;

public class Cli {

    private static final Logger LOG = Logger.getLogger(Cli.class);

    private static final String PERFORM_UPGRADES = "perform-upgrades";
    private static final String GENERATE_PRS = "generate-prs";
    private static final String GENERATE_REPORT = "generate-report";
    private static final String GENERATE_HTML_REPORT = "generate-html-report";
    private static final String SEND_HTML_REPORT = "send-html-report";
    private static final String GENERATE_CONFIG = "generate-config";
    private static final String CHECK_CONFIG = "check-config";
    private static final String[] COMMANDS = {PERFORM_UPGRADES, GENERATE_PRS, GENERATE_REPORT, GENERATE_HTML_REPORT,
            GENERATE_CONFIG, CHECK_CONFIG, SEND_HTML_REPORT};

    private static final String PREFIX_DOESNT_MATCH_MSG = "Dependency %s doesn't match prefix '%s'";

    final private Options options;
    final private CommandLineParser parser = new DefaultParser();
    private Configuration configuration;
    private Collection<ScopedArtifactRef> rootProjectDependencies;

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
        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .desc("output file for report generation")
                .build());
        options.addOption(Option.builder().longOpt("email-from")
                .desc("From address, when sending report as an email")
                .hasArgs()
                .numberOfArgs(1)
                .build());
        options.addOption(Option.builder().longOpt("email-to")
                .desc("Address to send the email report to")
                .hasArgs()
                .numberOfArgs(1)
                .build());
        options.addOption(Option.builder().longOpt("email-smtp-host")
                .desc("SMTP server host")
                .hasArgs()
                .numberOfArgs(1)
                .build());
        options.addOption(Option.builder().longOpt("email-smtp-port")
                .desc("SMTP server port")
                .hasArgs()
                .numberOfArgs(1)
                .build());
        options.addOption(Option.builder().longOpt("email-subject")
                .desc("Report email subject")
                .hasArgs()
                .numberOfArgs(1)
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

        File configurationFile = null;
        if (cmd.hasOption('c')) {
            configurationFile = new File(cmd.getOptionValue('c'));
        }

        if (!cmd.hasOption('f')) {
            System.err.println("Missing option 'f'");
            printHelp();
            return 10;
        }
        File pomFile = new File(cmd.getOptionValue('f'));

        rootProjectDependencies = new PmeDependencyCollector(pomFile).getRootProjectDependencies();

        boolean success;
        if (PERFORM_UPGRADES.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);
            success = performAlignment(new ModifyLocallyProcessingStrategy(pomFile));
        } else if (GENERATE_PRS.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);
            UpgradeProcessingStrategy strategy = new SeparatePRsProcessingStrategy(configuration, pomFile);
            success = performAlignment(strategy);
        } else if (GENERATE_REPORT.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);
            UpgradeProcessingStrategy strategy;
            if (cmd.hasOption('o')) {
                strategy = new TextReportProcessingStrategy(configuration, pomFile, cmd.getOptionValue('o'));
            } else {
                strategy = new TextReportProcessingStrategy(configuration, pomFile, System.out);
            }
            success = performAlignment(strategy);
        } else if (GENERATE_HTML_REPORT.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);
            UpgradeProcessingStrategy strategy;
            if (cmd.hasOption('o')) {
                strategy = new HtmlReportProcessingStrategy(configuration, pomFile, cmd.getOptionValue('o'));
            } else {
                strategy = new HtmlReportProcessingStrategy(configuration, pomFile, System.out);
            }
            success = performAlignment(strategy);
        } else if (SEND_HTML_REPORT.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);

            String host = cmd.getOptionValue("email-smtp-host");
            if (Strings.isNullOrEmpty(host)) {
                System.err.println("Missing option email-smtp-host.");
                printHelp();
                return 10;
            }
            String port = cmd.getOptionValue("email-smtp-port");
            if (Strings.isNullOrEmpty(port)) {
                System.err.println("Missing option email-smtp-port.");
                printHelp();
                return 10;
            }
            String from = cmd.getOptionValue("email-from");
            if (Strings.isNullOrEmpty(from)) {
                System.err.println("Missing option email-from.");
                printHelp();
                return 10;
            }
            String to = cmd.getOptionValue("email-to");
            if (Strings.isNullOrEmpty(to)) {
                System.err.println("Missing option email-to.");
                printHelp();
                return 10;
            }
            String subject = cmd.getOptionValue("email-subject");
            if (Strings.isNullOrEmpty(subject)) {
                System.err.println("Missing option email-subject.");
                printHelp();
                return 10;
            }
            String outputFile;
            if (cmd.hasOption('o')) {
                outputFile = cmd.getOptionValue('o');
            } else {
                outputFile = "report.html";
            }

            UpgradeProcessingStrategy strategy = new EmailReportProcessingStrategy(configuration, pomFile,
                    host, port, from, to, subject, outputFile);
            success = performAlignment(strategy);
        } else if (GENERATE_CONFIG.equals(arguments[0])) {
            new ConfigurationGenerator().generateDefautlConfig(configurationFile, rootProjectDependencies);
            success = true;
        } else if (CHECK_CONFIG.equals(arguments[0])) {
            configuration = new Configuration(configurationFile);
            Collection<Pair<ScopedArtifactRef, String>> outOfDate =
                    configuration.findOutOfDateRestrictions(rootProjectDependencies);
            outOfDate.forEach(p ->
                    System.err.printf((PREFIX_DOESNT_MATCH_MSG) + "%n", p.getLeft(), p.getRight())
            );
            success = true;
        } else {
            System.err.println("Unknown action: " + arguments[0]);
            printHelp();
            return 10;
        }

        if (success) {
            return 0;
        } else {
            return 11;
        }
    }

    private void printHelp() {
        String header = "\nCommands:\n" +
                "  generate-report      Generates a text report\n" +
                "  generate-html-report Generates an HTML report\n" +
                "  send-html-report     Send an HTML report via e-mail\n" +
                "  perform-upgrades     Locally perform dependencies in given POM file\n" +
                "  generate-prs         Aligns dependencies in given POM file and generates\n" +
                "                       separate pull requests for each upgrade\n" +
                "  generate-config      Generates somewhat sane base for dependency alignment\n" +
                "                       configuration for given project\n" +
                "  check-config         Checks if configuration is up-to-date\n" +
                "                       (project dependencies match configured version\n" +
                "                       prefixes)\n" +
                "\n" +
                "Parameters:\n";
        //      |<--- Line height of 74 chars                                          --->|

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <path/to/cli.jar> <command> <params>", header, options, "");
    }

    private boolean performAlignment(UpgradeProcessingStrategy strategy) {
        assert configuration != null;
        assert rootProjectDependencies != null;

        LoggerClient loggerClient = null;
        if (configuration.getLogger().isSet()) {
            LOG.infof("Logger service URI is %s", configuration.getLogger().getUri());
            LOG.infof("Logger project code is %s", configuration.getLogger().getProjectCode());
            loggerClient = LoggerClientFactory.createClient(URI.create(configuration.getLogger().getUri()));
        } else {
            LOG.infof("Logger service not configured.");
        }

        AvailableVersionsResolver availableVersionsResolver = new DefaultAvailableVersionsResolver(configuration);
        DependencyEvaluator evaluator = new DependencyEvaluator(configuration, availableVersionsResolver, loggerClient);
        List<ArtifactResult<ComponentUpgrade>> scopedUpgrades =
                evaluator.getVersionsToUpgrade(rootProjectDependencies);
        try {
            return strategy.process(scopedUpgrades);
        } catch (Exception e) {
            LOG.error("Error when processing component upgrades", e);
            return false;
        }
    }

}
