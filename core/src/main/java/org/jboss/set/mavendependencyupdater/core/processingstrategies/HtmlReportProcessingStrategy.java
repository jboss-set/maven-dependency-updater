package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.jboss.set.mavendependencyupdater.DependencyEvaluator.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;

import java.io.File;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static j2html.TagCreator.a;
import static j2html.TagCreator.caption;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.text;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;
import static j2html.TagCreator.ul;

/**
 * Prints upgradable dependencies report to stdout or to given file.
 * <p>
 * Non thread safe.
 */
public class HtmlReportProcessingStrategy extends TextReportProcessingStrategy {

    private static final String BASIC_STYLES = "font-family: Verdana,sans-serif;" +
            "font-size: 10pt;";
    private static final String TABLE_STYLES = "margin: 2em 0;" +
            "border-collapse: collapse;";
    private static final String CAPTION_STYLES = "text-align: left;" +
            "font-weight: bold;";
    private static final String TH_TD_STYLES = "border-bottom: 1px solid #ddd;" +
            "padding: 10px;" +
            "text-align: left;";
    private static final String GAV_STYLES = "font-family: \"Courier New\";";
    private static final String UL_STYLES = "list-style-type: circle;";
    private static final String LI_STYLES = "margin: 7px 0;";
    private static final String REPO_LABEL_STYLES = "border-radius: 5px;" +
            "padding: 3px;";
    private static final String FOOTER_STYLES = "color: #999;";

    private static final String BG_NEW = "background-color: #fffeec;";

    private static final String BG1 = "background-color: #a8df65;";
    private static final String BG2 = "background-color: #edf492;";
    private static final String BG3 = "background-color: #efb960;";
    private static final String BG4 = "background-color: #ee91bc;";

    private static final String[] BACKGROUNDS = {BG1, BG2, BG3, BG4};

    private List<String> repositoryKeys;

    public HtmlReportProcessingStrategy(Configuration configuration, File pomFile, PrintStream printStream) {
        super(configuration, pomFile, printStream);
        initRepositoryKeys();
    }

    public HtmlReportProcessingStrategy(Configuration configuration, File pomFile, String outputFileName) {
        super(configuration, pomFile, outputFileName);
        initRepositoryKeys();
    }

    @Override
    public boolean process(List<ComponentUpgrade> upgrades) {
        try {
            if (upgrades.size() == 0) {
                LOG.info("No components to upgrade.");
                return true;
            }
            initOutputStream();

            List<ComponentUpgrade> sortedUpgrades =
                    upgrades.stream().sorted(new ComponentUpgradeComparator())
                            .collect(Collectors.toList());

            String html = div().withStyle(BASIC_STYLES).with(
                    h2("Component Upgrade Report"),
                    p("Following repositories were searched:"),
                    ul().withStyle(UL_STYLES).with(
                            each(configuration.getRepositories().entrySet(),
                                    entry -> li().withStyle(LI_STYLES).with(
                                            span(entry.getKey())
                                                    .withStyle(REPO_LABEL_STYLES + repositoryColor(entry.getKey())),
                                            text(" " + entry.getValue())
                                    ))
                    ),
                    table().withStyle(BASIC_STYLES + TABLE_STYLES).with(
                            caption("Possible Component Upgrades").withStyle(CAPTION_STYLES),
                            thead(tr().with(
                                    th("GAV").withStyle(TH_TD_STYLES),
                                    th("New Version").withStyle(TH_TD_STYLES),
                                    th("Repository").withStyle(TH_TD_STYLES),
                                    th("Since").withStyle(TH_TD_STYLES)
                            )),
                            each(sortedUpgrades, upgrade -> {
                                boolean isNew = upgrade.getFirstSeen() == null;
                                return tr().with(
                                        td(upgrade.getArtifact().getGroupId()
                                                + ":" + upgrade.getArtifact().getArtifactId()
                                                + ":" + upgrade.getArtifact().getVersionString())
                                                .withStyle(TH_TD_STYLES + GAV_STYLES + (isNew ? BG_NEW : "")),
                                        td(upgrade.getNewVersion())
                                                .withStyle(TH_TD_STYLES + (isNew ? BG_NEW : "")),
                                        td(span(upgrade.getRepository())
                                                .withStyle(REPO_LABEL_STYLES + repositoryColor(upgrade.getRepository())))
                                                .withStyle(TH_TD_STYLES + (isNew ? BG_NEW : "")),
                                        td(upgrade.getFirstSeen() == null ? "new" : upgrade.getFirstSeen().format(DATE_FORMATTER))
                                                .withStyle(TH_TD_STYLES + (isNew ? BG_NEW : ""))
                                );
                            }),
                            tr(td(sortedUpgrades.size() + " items").withStyle(TH_TD_STYLES).attr("colspan", "4"))),
                    p("Generated on " + DATE_FORMATTER.format(ZonedDateTime.now())),
                    p().withStyle(FOOTER_STYLES).with(
                            text("Report generated by "),
                            a("Maven Dependency Updater")
                                    .withHref(PROJECT_URL)
                                    .withStyle(FOOTER_STYLES)
                    )
            ).render();
            outputStream.println(html);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Report generation failed", e);
        } finally {
            if (outputStream != null && outputStream != System.out) {
                outputStream.close();
            }
        }
    }

    private void initRepositoryKeys() {
        repositoryKeys = new ArrayList<>(configuration.getRepositories().keySet());
    }

    private String repositoryColor(String key) {
        int idx = repositoryKeys.indexOf(key);
        return BACKGROUNDS[idx % BACKGROUNDS.length];
    }

}
