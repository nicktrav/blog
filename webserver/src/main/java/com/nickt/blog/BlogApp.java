package com.nickt.blog;

import com.nickt.blog.container.AppContainer;
import com.nickt.blog.webserver.GenericServerModule;

/**
 * Entry point into the webserver for the nicktrave.rs Blog.
 */
// TODO: Config
public class BlogApp {

  public static void main(String[] args) throws Exception {
    new AppContainer.Builder()
        .withModules(new GenericServerModule())
        .build()
        .run();
  }
}
