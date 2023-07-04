package org.jboss.set.mavendependencyupdater.core.aggregation;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
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
     * <p>
     * E.g. if a subset of component upgrades would lead to upgrading of the same property, only the first component
     * upgrade in this subset would be present in the resulting list.
     *
     * @param pomFile POM file being updated
     * @param scopedUpgrades list of component upgrades to be aggregated
     * @return aggregated list
     */
    public static List<ArtifactResult<ComponentUpgrade>> aggregateComponentUpgrades(File pomFile, List<ArtifactResult<ComponentUpgrade>> scopedUpgrades)
            throws IOException, XmlPullParserException {
        final URI uri = pomFile.toURI();
        final Set<ModifiedProperty> modifiedProperties = new HashSet<>();
        final List<ArtifactResult<ComponentUpgrade>> aggregatedUpgrades = new ArrayList<>();

        for (ArtifactResult<ComponentUpgrade> scopedUpgrade: scopedUpgrades) {
            Optional<LocatedDependency> locatedDependencyOpt =
                    PomDependencyUpdater.locateDependency(pomFile, scopedUpgrade.getArtifactRef());
            if (locatedDependencyOpt.isPresent()) {
                LocatedDependency locatedDependency = locatedDependencyOpt.get();
                Optional<ComponentUpgrade> upgrade = scopedUpgrade.getAny();
                @SuppressWarnings("OptionalIsPresent")
                boolean added = modifiedProperties.add(
                        new ModifiedProperty(uri, locatedDependency.getProfile(), locatedDependency.getVersionProperty(),
                                upgrade.isPresent() ? upgrade.get().getNewVersion() : null));
                if (added) {
                    aggregatedUpgrades.add(scopedUpgrade);
                }
            }
        }

        return aggregatedUpgrades;
    }
}
