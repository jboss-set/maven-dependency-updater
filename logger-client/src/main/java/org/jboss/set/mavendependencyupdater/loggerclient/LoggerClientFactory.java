package org.jboss.set.mavendependencyupdater.loggerclient;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;

public class LoggerClientFactory {

    public static LoggerClient createClient(URI baseURI) {
        return RestClientBuilder.newBuilder()
                .baseUri(baseURI)
                .register(LoggerResponseExceptionMapper.class)
                .build(LoggerClient.class);
    }
}
