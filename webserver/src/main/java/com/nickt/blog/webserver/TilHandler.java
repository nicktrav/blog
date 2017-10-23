package com.nickt.blog.webserver;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Handler for the "Today I learned" content.
 */
@Singleton
@Path("/til")
public class TilHandler {

  private static final String TEMPLATE = "main.html";
  private static final String PATH_PREFIX = "/pages/til";

  private final Mustache template;

  @Inject public TilHandler(MustacheFactory mustacheFactory) {
    this.template = mustacheFactory.compile(TEMPLATE);
  }

  @GET
  @Produces("text/html")
  public Response index() throws IOException {
    StringWriter stringWriter = new StringWriter();
    template.execute(stringWriter, new TilPage("TIL", getContent("index")));

    return okResponse(stringWriter.toString());
  }

  @GET
  @Path("preamble")
  @Produces("text/html")
  public Response getPreamble() throws IOException {
    StringWriter stringWriter = new StringWriter();
    template.execute(stringWriter, new TilPage("Preamble", getContent("preamble")));

    return okResponse(stringWriter.toString());
  }

  @GET
  @Path("{date : \\d{4}-\\d{2}-\\d{2}}-{title}")
  @Produces("text/html")
  public Response getResponse(@PathParam("date") String date, @PathParam("title") String title)
      throws IOException {
    StringWriter stringWriter = new StringWriter();
    template.execute(stringWriter, new TilPage(date, getContent(date + "-" + title)));

    return okResponse(stringWriter.toString());
  }

  private String getContent(String file) throws IOException {
    InputStream content = this.getClass().getResourceAsStream(PATH_PREFIX + "/" + file + ".html");
    return CharStreams.toString(new InputStreamReader(content));
  }

  private static Response okResponse(Object content) {
    return Response
        .status(Response.Status.OK)
        .entity(content)
        .build();
  }
}
