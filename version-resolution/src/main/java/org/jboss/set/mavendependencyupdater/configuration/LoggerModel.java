package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * Configuration of external logger service that records generated component upgrades.
 */
public class LoggerModel {

    private static final String PROJECT_CODE = "logger.projectCode";
    private static final String LOGGER_URI = "logger.uri";

    @JsonProperty
    private String uri;

    @JsonProperty
    private String projectCode;

    public String getUri() {
        if (System.getProperties().containsKey(LOGGER_URI)) {
            return System.getProperty(LOGGER_URI);
        }
        return uri;
    }

    public String getProjectCode() {
        if (System.getProperties().containsKey(PROJECT_CODE)) {
            return System.getProperty(PROJECT_CODE);
        }
        return projectCode;
    }

    /**
     * Is logging functionality active?
     */
    public boolean isActive() {
        return StringUtils.isNoneBlank(getUri(), getProjectCode());
    }
}
