package org.jboss.set.mavendependencyupdater.loggerclient;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class LoggerClientFactory {

    private static LoggerClient NOOP_LOGGER_CLIENT = new LoggerClient() {
        @Override
        public List<ComponentUpgradeDTO> getAll(String project) {
            return Collections.emptyList();
        }

        @Override
        public ComponentUpgradeDTO getFirst(String project, String groupId, String artifactId, String newVersion) throws UpgradeNotFoundException {
            throw new UpgradeNotFoundException();
        }

        @Override
        public void create(List<ComponentUpgradeDTO> componentUpgrades) {
        }
    };

    public static LoggerClient createClient(URI baseURI) {
        return RestClientBuilder.newBuilder()
                .baseUri(baseURI)
                .register(LoggerResponseExceptionMapper.class)
                .build(LoggerClient.class);
    }

    public static LoggerClient createNoOpClient() {
        return NOOP_LOGGER_CLIENT;
    }
}
