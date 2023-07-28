package org.jboss.set.mavendependencyupdater.core.aggregation;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.LocatedDependency;
import org.jboss.set.mavendependencyupdater.LocatedProperty;
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
     * @param upgrades list of component upgrades to be aggregated
     * @return aggregated list
     */
    public static List<ArtifactResult<ComponentUpgrade>> aggregateComponentUpgrades(File rootPom, List<ArtifactResult<ComponentUpgrade>> upgrades)
            throws IOException, XmlPullParserException {
        final Set<LocatedProperty> modifiedProperties = new HashSet<>();
        final List<ArtifactResult<ComponentUpgrade>> aggregatedUpgrades = new ArrayList<>();

        for (ArtifactResult<ComponentUpgrade> artifactUpgrade: upgrades) {
            Optional<ComponentUpgrade> componentUpgradeOptional = artifactUpgrade.getAny();
            if (!componentUpgradeOptional.isPresent()) {
                continue;
            }
            ComponentUpgrade upgrade = componentUpgradeOptional.get();

            File pomFile;
            Optional<LocatedDependency> locatedDependencyOpt;
            if (upgrade.getProject() != null) {
                pomFile = upgrade.getProject().getPom();
                locatedDependencyOpt = PomDependencyUpdater.locateDependency(upgrade.getProject(), artifactUpgrade.getArtifactRef());
            } else {
                pomFile = rootPom;
                locatedDependencyOpt = PomDependencyUpdater.locateDependency(pomFile, artifactUpgrade.getArtifactRef());
            }

            if (locatedDependencyOpt.isPresent()) {
                LocatedDependency locatedDependency = locatedDependencyOpt.get();
                LocatedProperty locatedProperty = locatedDependency.getLocatedProperty();
                boolean added = modifiedProperties.add(locatedProperty);
                if (added) {
                    aggregatedUpgrades.add(artifactUpgrade);
                }
            }
        }

        return aggregatedUpgrades;
    }
}
