package org.jboss.set.mavendependencyupdater.core.aggregation;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URI;

/**
 * Identifies a property in a particular POM file and a profile.
 *
 * It's used to record properties that would be changed by a component upgrade, to detect that given property would
 * have already been changed by a previous component upgrade.
 */
class ModifiedProperty {

    final private URI pomUri;
    final private String profile;
    final private String propertyName;
    final private String newValue;

    ModifiedProperty(URI pomUri, String profile, String propertyName, String newValue) {
        this.pomUri = pomUri;
        this.profile = profile;
        this.propertyName = propertyName;
        this.newValue = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ModifiedProperty that = (ModifiedProperty) o;

        return new EqualsBuilder()
                .append(pomUri, that.pomUri)
                .append(profile, that.profile)
                .append(propertyName, that.propertyName)
                .append(newValue, that.newValue)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(pomUri)
                .append(profile)
                .append(propertyName)
                .append(newValue)
                .toHashCode();
    }
}
