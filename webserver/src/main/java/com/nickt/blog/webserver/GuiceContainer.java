package com.nickt.blog.webserver;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import javax.servlet.ServletException;

/**
 * A {@link ServletContainer} that provides easier binding of annotated {@link Providers} and
 * {@link Resources} classes.
 */
@Singleton
class GuiceContainer extends ServletContainer {

  private final Set<Class> providers;
  private final Set<Class> resources;
  private final Injector injector;

  @Inject
  public GuiceContainer(@Providers Set<Class> providers, @Resources Set<Class> resources,
      Injector injector) {
    this.providers = providers;
    this.resources = resources;
    this.injector = injector;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ResourceConfig getDefaultResourceConfig(Map<String, Object> props,
      WebConfig webConfig) throws ServletException {
        Set<Class<?>> classes = (Set) Sets.union(providers, resources);
    return new DefaultResourceConfig(classes);
  }

  @Override
  protected void initiate(ResourceConfig config, WebApplication webApp) {
    IoCComponentProviderFactory factory = new GuiceComponentProviderFactory(config, injector);
    webApp.initiate(config, factory);
  }
}
