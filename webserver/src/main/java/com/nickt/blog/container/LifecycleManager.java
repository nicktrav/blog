package com.nickt.blog.container;

import com.google.common.base.Preconditions;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for transitioning the application between various phases of execution.
 */
@Singleton class LifecycleManager {

  private static final Logger logger = LogManager.getLogger(LifecycleManager.class);

  enum LifecycleStatus {
    /**
     * Applications start in this state.
     */
    IDLE,

    /**
     * The application is in the process of starting up. An intermediate state in which the
     * #handleStarting callbacks are called.
     */
    STARTING,

    /**
     * The steady state of the application.
     */
    RUNNING,

    /**
     * The application is in the process of shutting down. An intermediate state in which the
     * #handleStopping callbacks are called.
     */
    STOPPING,

    /**
     * The penultimate state for the application. Nothing application-specific should occur after
     * this point. The VM is ready to shutdown.
     */
    STOPPED,

    /**
     * Shutting down. Signal that it is OK for the VM to exit.
     */
    SHUTTING_DOWN,
  }

  private final Set<LifecycleListener> lifecycleListeners;
  private LifecycleStatus status;

  @Inject LifecycleManager(Set<LifecycleListener> lifecycleListeners) {
    this.lifecycleListeners = lifecycleListeners;
    this.status = LifecycleStatus.IDLE;
  }

  /**
   * Move from {@link LifecycleStatus#IDLE} to {@link LifecycleStatus#RUNNING} by way of the
   * {@link LifecycleStatus#STARTING} state.
   */
  void start() {
    Preconditions.checkState(status == LifecycleStatus.IDLE, "Can only start from IDLE state");
    status = LifecycleStatus.STARTING;

    try {
      for (LifecycleListener listener : lifecycleListeners) {
        listener.handleStarting();
      }
    } catch (Throwable e) {
      logger.error("Caught exception starting {}", e);
      throw e;
    }

    status = LifecycleStatus.RUNNING;
  }

  /**
   * Move from {@link LifecycleStatus#RUNNING} to {@link LifecycleStatus#STOPPED} by way of the
   * {@link LifecycleStatus#STOPPING} state.
   */
  void stop() {
    Preconditions.checkState(status == LifecycleStatus.RUNNING, "Can only stop from RUNNING state");
    status = LifecycleStatus.STOPPING;

    for (LifecycleListener listener : lifecycleListeners) {
      try {
        listener.handleStopping();
      } catch (Exception e) {
        logger.error("Caught exception stopping {}: {}", listener.getClass(), e);
      }
    }

    status = LifecycleStatus.STOPPED;
  }

  /**
   * Registers a hook to be called when the VM has received the signal to stop. This hook involves
   * calling stop and shutdown on the lifecycle.
   */
  void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook called. Stopping and shutting down.");
      stop();
      shutdown();
    }));
  }

  /**
   * Calls the shutdown hooks for the application.
   */
  private void shutdown() {
    Preconditions.checkState(status == LifecycleStatus.STOPPED,
        "Can only shut down from a STOPPED state");
    status = LifecycleStatus.SHUTTING_DOWN;

    for (LifecycleListener listener : lifecycleListeners) {
      try {
        listener.handleShutdown();
      } catch (Exception e) {
        logger.error("Saw exception shutting down {}. Continuing with shutdown. {} ",
            listener.getClass(), e);
      }
    }
  }

}
