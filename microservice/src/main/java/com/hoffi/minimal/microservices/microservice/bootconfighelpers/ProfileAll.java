package com.hoffi.minimal.microservices.microservice.bootconfighelpers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(ProfileAllCondition.class)
public @interface ProfileAll {

    /** The set of profiles for which the annotated component should be registered. */
    String[] value();

}
