package com.nickt.blog.webserver;

/**
 * POJO representing a "Today I learned" page, that can be passed to a template for rendering.
 */
class TilPage {

  private final String title;
  private final String body;

  TilPage(String title, String body) {
    this.title = title;
    this.body = body;
  }

  String title() {
    return title;
  }

  String body() {
    return body;
  }
}
