package com.nickt.blog.webserver;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Bridges the gap between Guice and the HK2 DI frameworks to allow for injection via Guice into
 * JAX-RS classes that are used by Jersey.
 *
 * <p>Implementing classes should bind the respective handlers in the overridden
 * {@link #configure()} method.
 */
public abstract class AbstractGuiceJerseyModule extends AbstractModule {

  @Override protected abstract void configure();

  /**
   * Bind an annotated JAX-RS class to be used by Jersey.
   */
  void bindClass(Class<?> klass) {
    resourceBinder().addBinding().toInstance(klass);
  }

  @Provides ResourceConfig providesResourceConfig(@Resources Set<Class> resources) {
    ResourceConfig resourceConfig = new ResourceConfig();
    resources.forEach(resourceConfig::register);

    return resourceConfig;
  }

  private Multibinder<Class> resourceBinder() {
    return Multibinder.newSetBinder(binder(), Class.class, Resources.class);
  }
}
