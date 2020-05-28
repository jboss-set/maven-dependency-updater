package org.jboss.set.mavendependencyupdater.loggerclient;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface LoggerClient {

    @GET
    @Path("/component-upgrades/{project}")
    List<ComponentUpgradeDTO> getAll(@PathParam("project") String project);

    @GET
    @Path("/component-upgrades/{project}/{groupId}/{artifactId}/{newVersion}")
    ComponentUpgradeDTO getFirst(@PathParam("project") String project, @PathParam("groupId") String groupId,
                                 @PathParam("artifactId") String artifactId, @PathParam("newVersion") String newVersion)
            throws UpgradeNotFoundException;

    @POST
    @Path("/component-upgrades/")
    Response create(List<ComponentUpgradeDTO> componentUpgrades);

}
