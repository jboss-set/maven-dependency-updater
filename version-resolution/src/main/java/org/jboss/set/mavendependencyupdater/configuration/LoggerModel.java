package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoggerModel {

    @JsonProperty
    private String uri;

    @JsonProperty
    private String projectCode;

    public String getUri() {
        return uri;
    }

    public String getProjectCode() {
        return projectCode;
    }
}
