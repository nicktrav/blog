package com.nickt.blog.container;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Container for launching long-running applications.
 */
// TODO(nickt): Config
public class AppContainer {

  private static final Logger logger = LogManager.getLogger(AppContainer.class);

  private final List<Module> modules;
  private final CountDownLatch shutdownLatch;

  private AppContainer(Builder builder) {
    this.modules = builder.modules;
    this.shutdownLatch = new CountDownLatch(1);
  }

  /**
   * Launch a the long running {@link AppContainer}.
   */
  public void run() {
    setupLogging();

    // Provide additional modules
    modules.add(new LifecycleModule() {
      @Override protected void configureLifecycleListeners() {
        bindLifecycleListener().toInstance(new SimpleLifecycleListener() {
          @Override public void handleShutdown() {
            shutdownLatch.countDown();
          }
        });
      }
    });

    Injector injector = Guice.createInjector(modules);

    LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
    lifecycleManager.registerShutdownHook();
    lifecycleManager.start();

    awaitShutdown();
  }

  private void awaitShutdown() {
    Uninterruptibles.awaitUninterruptibly(shutdownLatch);
  }

  private void setupLogging() {
    Logger root = LogManager.getRootLogger();
    Configurator.setLevel(root.getName(), Level.INFO);


    logger.info("Building custom layout");
    ImmutableList.Builder<String> parts = ImmutableList.builder();
    PatternLayout.Builder builder = PatternLayout.newBuilder();
    parts.add("%d{ISO8601} %5p [%t] ");
    parts.add("%c{2} - %m%n%throwable");
    String patternStr = Joiner.on("").join(parts.build());

    logger.info("Using pattern {}", patternStr);
    builder.withPattern(patternStr);

    ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
        .withLayout(builder.build())
        .withName("root-console-appended")
        .build();

    org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) root;

    logger.info("Removing all existing appenders");
    for (Appender appender : coreLogger.getAppenders().values()) {
      coreLogger.removeAppender(appender);
    }

    logger.info("Adding console appender");
    coreLogger.addAppender(consoleAppender);
  }

  /**
   * Builder for an {@link AppContainer}.
   */
  public static class Builder {

    private List<Module> modules = new ArrayList<>();

    /**
     * Modules to use in this {@link AppContainer}.
     *
     * @param modules modules to use
     * @return the builder instance
     */
    public Builder withModules(Module... modules) {
      this.modules.addAll(Arrays.asList(modules));
      return this;
    }

    /**
     * @return an instance of an {@link AppContainer}
     */
    public AppContainer build() {
      return new AppContainer(this);
    }
  }
}
