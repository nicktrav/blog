package com.nickt.blog.container;

/**
 * Listens for events in the lifecyle of an application.
 */
public interface LifecycleListener {

  /**
   * Handle transitioning between the {@link LifecycleManager.LifecycleStatus#IDLE} and
   * {@link LifecycleManager.LifecycleStatus#RUNNING} phases. The application is in the
   * {@link LifecycleManager.LifecycleStatus#STARTING} phase during this time.
   */
  void handleStarting();

  /**
   * Handle transitioning between the {@link LifecycleManager.LifecycleStatus#RUNNING} and
   * {@link LifecycleManager.LifecycleStatus#STOPPED} phases. The application is in the
   * {@link LifecycleManager.LifecycleStatus#STOPPING} phase during this time.
   */
  void handleStopping();

  /**
   * Handle transitioning between the {@link LifecycleManager.LifecycleStatus#STOPPED} and the
   * shutdown phases. The application is in the
   * {@link LifecycleManager.LifecycleStatus#SHUTTING_DOWN} phase during this time.
   */
  void handleShutdown();
}
