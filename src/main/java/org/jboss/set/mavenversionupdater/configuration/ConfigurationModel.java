package org.jboss.set.mavenversionupdater.configuration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigurationModel {

    @JsonProperty
    private Map<String, String> streams;

    @JsonProperty(value = "bom-coordinates", required = true)
    private String bomCoordinates;

    public String getBomCoordinates() {
        return bomCoordinates;
    }

    public Map<String, String> getStreams() {
        return streams;
    }
}
