package com.hoffi.minimal.microservices.microservice.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ImplementationHint {

    /** The set of profiles for which the annotated component should be registered. */
    String[] value() default "";

    public Class<?> clazz();

    public String comment() default "";
}
