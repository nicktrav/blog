package com.nickt.blog.webserver;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.ServletModule;
import com.nickt.blog.container.LifecycleModule;
import com.nickt.blog.container.SimpleLifecycleListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Provider;
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
    install(new ServletModule());
    install(new AbstractGuiceJerseyModule() {
      @Override protected void configure() {
        bindClass(RootHandler.class);
        bindClass(TilHandler.class);
      }
    });

    Provider<Injector> injectorProvider = getProvider(Injector.class);
    bind(new TypeLiteral<Supplier<Injector>>() {}).toInstance(injectorProvider::get);

    install(new LifecycleModule() {
      @Override protected void configureLifecycleListeners() {
        bindLifecycleListener().to(ServerLifecycleListener.class);
      }
    });

    bind(MustacheFactory.class).toInstance(new DefaultMustacheFactory("templates"));
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
