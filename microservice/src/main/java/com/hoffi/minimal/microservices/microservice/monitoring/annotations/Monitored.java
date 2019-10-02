package com.hoffi.minimal.microservices.microservice.monitoring.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    // @AliasFor("target")
    public String value();

    // @AliasFor("value")
    // public String id();
}