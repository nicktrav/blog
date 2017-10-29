package com.nickt.blog.webserver;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;

/**
 * Handler for static content, served from the {@code /static} path.
 */
@Singleton
public class StaticContentServlet extends HttpServlet {

  private static final String MIME_TYPES_PATH = "/mime.types";
  private static final String STATIC_PATH = "/static";
  private static final String GZIP = "gzip";
  private static final CacheControl CACHE_CONTROL = HttpUtils.cacheControlMaxAge(7, TimeUnit.DAYS);

  private final FileTypeMap mimeMap;

  StaticContentServlet() {
    try (InputStream in = StaticContentServlet.class.getResourceAsStream(MIME_TYPES_PATH)) {
      mimeMap = new MimetypesFileTypeMap(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = request.getPathInfo();
    InputStream inputStream = this.getClass().getResourceAsStream(STATIC_PATH + path);

    if (inputStream == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    response.setContentType(mimeMap.getContentType(path));
    response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL.toString());

    OutputStream responseStream = response.getOutputStream();

    // Compress as GZIP if requested
    String acceptHeader = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
    if (acceptHeader != null && acceptHeader.contains(GZIP)) {
      response.setHeader(HttpHeaders.CONTENT_ENCODING, GZIP);
      responseStream = new GZIPOutputStream(responseStream);
    }

    ByteStreams.copy(inputStream, responseStream);
    responseStream.close();
  }
}
