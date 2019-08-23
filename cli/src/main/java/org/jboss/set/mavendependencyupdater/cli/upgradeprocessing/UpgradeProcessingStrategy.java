package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.util.Map;

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
    boolean process(Map<ArtifactRef, String> upgrades);
}
