package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.jboss.set.mavendependencyupdater.DependencyEvaluator;

import java.util.List;

/**
 * A strategy that processes a list of component upgrades.
 *
 * It could for instance update pom.xml locally, or generate PRs, etc.
 */
public interface UpgradeProcessingStrategy {

    /**
     * Process component upgrades
     *
     * @param upgrades component upgrades
     * @return processed successfully?
     */
    boolean process(List<DependencyEvaluator.ComponentUpgrade> upgrades);
}
