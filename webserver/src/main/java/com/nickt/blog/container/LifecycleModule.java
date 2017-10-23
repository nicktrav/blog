package com.nickt.blog.container;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

/**
 * Installable module to which a {@link LifecycleListener} can be bound.
 *
 * <p>Implementing classes must provide the {@link #configureLifecycleListeners()} method, which
 * should call {@link #bindLifecycleListener()} to bind a lifecylce listener. For example:
 * <pre><code>
 * new LifecycleModule() {
 *   {@literal @}Override protected void configureLifecycleListeners() {
 *     bindLifecycleListener().toInstance(new SimpleLifecycleListener() {
 *       {@literal @}Override public void handleShutdown() {
 *         shutdownLatch.countDown();
 *       }
 *     });
 *   }
 * }</code></pre>
 */
public abstract class LifecycleModule extends AbstractModule {

  private Multibinder<LifecycleListener> lifecycleListeners;

  @Override protected void configure() {
    lifecycleListeners = Multibinder.newSetBinder(binder(), LifecycleListener.class);

    configureLifecycleListeners();
  }

  abstract protected void configureLifecycleListeners();

  protected LinkedBindingBuilder<LifecycleListener> bindLifecycleListener() {
    return lifecycleListeners.addBinding();
  }
}
