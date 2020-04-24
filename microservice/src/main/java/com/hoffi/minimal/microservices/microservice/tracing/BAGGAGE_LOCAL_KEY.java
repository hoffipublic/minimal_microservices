package com.hoffi.minimal.microservices.microservice.tracing;

import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
import org.springframework.cloud.sleuth.autoconfig.TraceEnvironmentPostProcessor;

/** enum for baggage.local-fields (keys) defined in application.(properties|yml) that ARE NOT(!) propagated downstream */
@ImplementationHint(clazz = TraceEnvironmentPostProcessor.class,
        comment = "contains the logpattern (template) used in application.yml logging.pattern property")
public enum BAGGAGE_LOCAL_KEY {
    // MDC only baggage items --> not propagated downstream, and may change multiple times within nested spans
    /**
     * all keys have to be defined in application.(properties|yml) (!!!!!!!)<br/>
     * spring.sleuth.baggage.local-fields<br/>
     * keys that also should appear in logging MDC also have to be listed in application.(properties|yml) (!!!!!!!!!)<br/>
     * spring.sleuth.baggage.correlation-fields<br/>
     * They have to also to appear EXACTLY the same in the logging.pattern of application.(properties|yml)
     */
    INSTANCE("i"), OPERATION("op"), CHUNK("chunk");

    private String mdcKey;

    BAGGAGE_LOCAL_KEY(String tag) {
        this.mdcKey = tag;
    }

    @Override
    public String toString() {
        return mdcKey;
    }
}

