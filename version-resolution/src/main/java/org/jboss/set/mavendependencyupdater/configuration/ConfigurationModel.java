package org.jboss.set.mavendependencyupdater.configuration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigurationModel {

    @JsonProperty
    private Map<String, String> streams;

    @JsonProperty(value = "bom-coordinates")
    private String bomCoordinates;

    /**
     * GAV of the BOM that's going to be generated.
     */
    public String getBomCoordinates() {
        return bomCoordinates;
    }

    public Map<String, String> getStreams() {
        return streams;
    }
}
