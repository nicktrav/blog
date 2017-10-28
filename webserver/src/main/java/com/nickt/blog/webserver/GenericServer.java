package com.nickt.blog.webserver;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.nickt.blog.webserver.GenericServerModule.JettyThreadPool;
import java.util.EnumSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Generic Jetty webserver.
 */
// TODO: Config
class GenericServer {

  private static final Logger logger = LogManager.getLogger(GenericServer.class);
  private static final String CLIENT_IP = "Client-IP";

  private volatile Server server;

  @Inject GenericServer(@JettyThreadPool ThreadPoolExecutor executor,
      Supplier<Injector> injectorSupplier) {
    this.server = new Server(new SizedExecutorThreadPool(executor));

    HttpConfiguration httpConfig = new HttpConfiguration();
    ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    httpConnector.setPort(9000);
    server.setConnectors(new ServerConnector[] {httpConnector});

    HandlerCollection handlers = new HandlerCollection();

    ServletHolder holder = new ServletHolder(ServletContainer.class);
    holder.setInitParameter("javax.ws.rs.Application", GuiceJerseyResourceConfig.class.getName());

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    webAppContext.addServlet(holder, "/*");
    webAppContext.setResourceBase("/");
    webAppContext.setContextPath("/");
    webAppContext.addEventListener(new GuiceServletContextListener() {
      @Override
      protected Injector getInjector() {
        return injectorSupplier.get();
      }
    });
    webAppContext.setServer(server);
    handlers.addHandler(webAppContext);

    RequestLogHandler loggingHandler = new RequestLogHandler();
    loggingHandler.setRequestLog((request, response) -> {
      long latency = System.currentTimeMillis() - request.getTimeStamp();
      logger.info(String.format("%s - %s %s %s %db %dms",
          getIp(request), request.getMethod(), request.getHttpURI(), response.getStatus(),
          response.getContentCount(), latency));
    });

    handlers.addHandler(loggingHandler);
    server.setHandler(handlers);
  }

  /**
   * Starts the Jetty webserver.
   *
   * @throws Exception
   */
  void start() throws Exception {
    server.start();
  }

  /**
   * Stops the Jetty webserver.
   *
   * @throws Exception
   */
  void stop() throws Exception {
    server.stop();
  }

  private static String getIp(HttpServletRequest request) {
    if (request.getHeader(CLIENT_IP) == null) {
      return request.getRemoteAddr();
    } else {
      return request.getHeader(CLIENT_IP);
    }
  }

  private static class SizedExecutorThreadPool extends ExecutorThreadPool implements
      ThreadPool.SizedThreadPool {

    private final ThreadPoolExecutor ex;

    SizedExecutorThreadPool(ThreadPoolExecutor ex) {
      super(ex);
      this.ex = ex;
    }

    @Override public int getMinThreads() {
      return ex.allowsCoreThreadTimeOut() ? 0 : ex.getCorePoolSize();
    }

    @Override public int getMaxThreads() {
      return ex.getMaximumPoolSize();
    }

    @Override public void setMinThreads(int threads) {
      throw new UnsupportedOperationException();
    }

    @Override public void setMaxThreads(int threads) {
      throw new UnsupportedOperationException();
    }
  }
}
