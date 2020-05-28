package org.jboss.set.mavendependencyupdater.loggerclient;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class LoggerResponseExceptionMapper implements ResponseExceptionMapper<UpgradeNotFoundException> {

    @Override
    public UpgradeNotFoundException toThrowable(Response response) {
        switch (response.getStatus()) {
            case 404:
                return new UpgradeNotFoundException();
        }
        return null;
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status == 404;
    }

}
