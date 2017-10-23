package com.nickt.blog.webserver;

import javax.ws.rs.core.CacheControl;

/**
 * Static utility methods and constants.
 */
class HttpUtils {

  private static final int CACHE_CONTROL_MAX_AGE_SECONDS = 86400; // 1 day

  static CacheControl CACHE_CONTROL = new CacheControl();
  static {
    CACHE_CONTROL.setMaxAge(CACHE_CONTROL_MAX_AGE_SECONDS);
  }

  // Static helper class
  private HttpUtils() {
  }
}
