package com.nickt.blog.webserver;

import com.google.inject.Injector;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

class GuiceJerseyResourceConfig extends ResourceConfig {

  @Inject
  public GuiceJerseyResourceConfig(ServiceLocator serviceLocator, ServletContext servletContext) {
    super(getResourceConfigFromGuice(servletContext));

    GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
    GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
    guiceBridge.bridgeGuiceInjector(getInjector(servletContext));
  }

  /**
   * Ask the {@link ServletContext} for an instance of the {@link Injector}.
   */
  private static Injector getInjector(ServletContext servletContext) {
    return (Injector) servletContext.getAttribute(Injector.class.getName());
  }

  /**
   * Return a {@link ResourceConfig} from the {@link ServletContext}.
   */
  private static ResourceConfig getResourceConfigFromGuice(ServletContext servletContext) {
    return getInjector(servletContext).getInstance(ResourceConfig.class);
  }
}
