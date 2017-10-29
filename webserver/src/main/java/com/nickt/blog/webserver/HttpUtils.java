package com.nickt.blog.webserver;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.CacheControl;

/**
 * Static utility methods and constants.
 */
class HttpUtils {

  /**
   * Returns a {@link CacheControl} for the given {@code maxAge}.
   */
  static CacheControl cacheControlMaxAge(long maxAge, TimeUnit timeUnit) {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(Math.toIntExact(timeUnit.toSeconds(maxAge)));

    return cacheControl;
  }

  // Static helper class
  private HttpUtils() {
  }
}
