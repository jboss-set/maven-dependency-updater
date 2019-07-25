package org.jboss.set.mavendependencyupdater.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigurationModel {

    @JsonProperty
    private Map<String, Object> rules;

    @JsonProperty(value = "bom-coordinates")
    private String bomCoordinates;

    @JsonProperty
    private List<String> ignoreScopes;

    /**
     * GAV of the BOM that's going to be generated.
     */
    public String getBomCoordinates() {
        return bomCoordinates;
    }

    public Map<String, Object> getRules() {
        return rules;
    }

    public List<String> getIgnoreScopes() {
        return ignoreScopes;
    }
}
