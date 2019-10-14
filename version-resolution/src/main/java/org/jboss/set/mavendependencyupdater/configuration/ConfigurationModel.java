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
    private Map<String, String> repositories;

    @JsonProperty
    private Map<String, Object> rules;

    @JsonProperty
    private List<String> ignoreScopes;

    @JsonProperty
    @JsonAlias("github")
    private GitHubConfigurationModel gitHub = new GitHubConfigurationModel();

    @JsonProperty
    private GitConfigurationModel git = new GitConfigurationModel();

    public Map<String, String> getRepositories() {
        return repositories;
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
