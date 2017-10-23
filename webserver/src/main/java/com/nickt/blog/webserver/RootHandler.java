package com.nickt.blog.webserver;

import java.net.URI;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Handler for the root path.
 */
@Singleton
@Path("/")
public class RootHandler {

  @GET
  public Response index() {
    return Response.temporaryRedirect(URI.create("/til")).build();
  }

  @GET
  @Path("_status")
  public Response status() {
    return Response.ok().build();
  }
}
