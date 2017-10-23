package com.nickt.blog.webserver;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;
import com.nickt.blog.container.LifecycleModule;
import com.nickt.blog.container.SimpleLifecycleListener;
import com.sun.jersey.guice.JerseyServletModule;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

/**
 * Module for providing a Jetty webserver, with a directory of templates that can be rendered.
 */
public class GenericServerModule extends AbstractModule {

  // TODO: Make these values configurable
  private static final int MIN_THREADS = 10;
  private static final int MAX_THREADS = 300;
  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  @Override protected void configure() {
    bind(MustacheFactory.class).toInstance(new DefaultMustacheFactory("templates"));

    Multibinder.newSetBinder(binder(), Class.class, Providers.class);
    Multibinder<Class> resourceBinder = Multibinder.newSetBinder(binder(), Class.class,
        Resources.class);

    resourceBinder.addBinding().toInstance(RootHandler.class);
    resourceBinder.addBinding().toInstance(TilHandler.class);

    install(new JerseyServletModule());
    install(new ServletModule() {
      @Override protected void configureServlets() {
        serve("/static/*").with(StaticContentServlet.class);
        serve("/*").with(GuiceContainer.class);
      }
    });

    install(new LifecycleModule() {
      @Override protected void configureLifecycleListeners() {
        bindLifecycleListener().to(ServerLifecycleListener.class);
      }
    });

    bind(GenericServer.class).in(Singleton.class);
  }

  /**
   * A {@link SimpleLifecycleListener} responsible for starting and stopping the webserver.
   */
  private static class ServerLifecycleListener extends SimpleLifecycleListener {

    private final GenericServer server;

    @Inject ServerLifecycleListener(GenericServer server) {
      this.server = server;
    }

    @Override public void handleStarting() {
      try {
        server.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void handleStopping() {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface JettyThreadPool {
  }

  /**
   * Provides a named {@link ThreadPoolExecutor} for use in the Jetty webserver.
   */
  @Provides @Singleton @JettyThreadPool ThreadPoolExecutor provideThreadPool() {
    ThreadFactory jettyThreadFactory =
        new ThreadFactoryBuilder().setNameFormat("jetty-thread-%d").build();
    return new ThreadPoolExecutor(MIN_THREADS, MAX_THREADS, SHUTDOWN_TIMEOUT_SECONDS,
        TimeUnit.SECONDS, new ArrayBlockingQueue<>(MAX_THREADS), jettyThreadFactory);
  }
}
