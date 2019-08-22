package org.jboss.set.mavendependencyupdater.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Overall configuration model, facilitates automatic conversion to/from JSON file.
 */
public class ConfigurationModel {

    public static final String IGNORE_SCOPES = "ignoreScopes";
    public static final String RULES = "rules";

    @JsonProperty
    private Map<String, Object> rules;

    @JsonProperty(value = "bom-coordinates")
    private String bomCoordinates;

    @JsonProperty
    private List<String> ignoreScopes;

    @JsonProperty
    @JsonAlias("github")
    private GitHubConfigurationModel gitHub;

    @JsonProperty
    private GitConfigurationModel git;

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

    public GitHubConfigurationModel getGitHub() {
        return gitHub;
    }

    public GitConfigurationModel getGit() {
        return git;
    }
}
