package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration model related to working with a local git repository.
 */
public class GitConfigurationModel {

    @JsonProperty
    private String remote;

    @JsonProperty
    private String baseBranch;

    public String getRemote() {
        return remote;
    }

    public String getBaseBranch() {
        return baseBranch;
    }
}
