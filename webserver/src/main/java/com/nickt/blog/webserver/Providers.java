package com.nickt.blog.webserver;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.ws.rs.ext.Provider;

@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@interface Providers {
}
