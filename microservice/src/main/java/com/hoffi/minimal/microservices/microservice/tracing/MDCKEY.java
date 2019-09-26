package com.hoffi.minimal.microservices.microservice.tracing;

import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
import org.springframework.cloud.sleuth.autoconfig.TraceEnvironmentPostProcessor;

@ImplementationHint(clazz = TraceEnvironmentPostProcessor.class,
        comment = "contains the logpattern (template) used in application.yml logging.pattern property")
public enum MDCKEY {
    // MDC only baggage items --> not propagated downstream, and may change multiple times within
    // nested spans
    INSTANCE("i"), OPERATION("op"), CHUNK("chunk");

    private String mdcKey;

    MDCKEY(String tag) {
        this.mdcKey = tag;
    }

    @Override
    public String toString() {
        return mdcKey;
    }
}

