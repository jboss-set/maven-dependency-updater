package org.jboss.set.mavendependencyupdater.core.aggregation;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.set.mavendependencyupdater.DependencyEvaluator.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.LocatedDependency;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ComponentUpgradeAggregator {

    /**
     * Aggregates component upgrades based on which properties they would update in the POM file.
     *
     * E.g. if a subset of component upgrades would lead to upgrading of the same property, only the first component
     * upgrade in this subset would be present in the resulting list.
     *
     * @param pomFile POM file being updated
     * @param upgrades list of component upgrades to be aggregated
     * @return aggregated list
     */
    public static List<ComponentUpgrade> aggregateComponentUpgrades(File pomFile, List<ComponentUpgrade> upgrades)
            throws IOException, XmlPullParserException {
        final URI uri = pomFile.toURI();
        final Set<ModifiedProperty> modifiedProperties = new HashSet<>();
        final List<ComponentUpgrade> aggregatedUpgrades = new ArrayList<>();

        for (ComponentUpgrade upgrade: upgrades) {
            Optional<LocatedDependency> locatedDependencyOpt =
                    PomDependencyUpdater.locateDependency(pomFile, upgrade.getArtifact());
            if (locatedDependencyOpt.isPresent()) {
                LocatedDependency locatedDependency = locatedDependencyOpt.get();
                boolean added = modifiedProperties.add(
                        new ModifiedProperty(uri, locatedDependency.getProfile(), locatedDependency.getVersionProperty(), upgrade.getNewVersion()));
                if (added) {
                    aggregatedUpgrades.add(upgrade);
                }
            }
        }

        return aggregatedUpgrades;
    }
}
