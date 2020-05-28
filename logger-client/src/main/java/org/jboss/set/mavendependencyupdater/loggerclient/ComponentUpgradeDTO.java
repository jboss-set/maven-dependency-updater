package org.jboss.set.mavendependencyupdater.loggerclient;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

/**
 * A data transfer object for JSON REST service.
 */
public class ComponentUpgradeDTO {

    public String project;
    public String groupId;
    public String artifactId;
    public String oldVersion;
    public String newVersion;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime created;

    @SuppressWarnings("unused")
    public ComponentUpgradeDTO() {
    }

    @SuppressWarnings("unused")
    public ComponentUpgradeDTO(String project, String groupId, String artifactId, String oldVersion, String newVersion, LocalDateTime created) {
        this.project = project;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.created = created;
    }

    @Override
    public String toString() {
        return "ComponentUpgradeDTO{" +
                "project='" + project + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", oldVersion='" + oldVersion + '\'' +
                ", newVersion='" + newVersion + '\'' +
                ", created=" + created +
                '}';
    }
}
