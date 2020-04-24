package com.hoffi.minimal.microservices.microservice.tracing;

import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
import org.springframework.cloud.sleuth.autoconfig.TraceEnvironmentPostProcessor;

/** enum for baggage.remote-fields (keys) defined in application.(properties|yml) that ARE propagated downstream */
@ImplementationHint(clazz = TraceEnvironmentPostProcessor.class,
        comment = "contains the logpattern (template) used in application.yml logging.pattern property")
public enum BAGGAGE_REMOTE_KEY {

    /**
     * all keys have to be defined in application.(properties|yml) (!!!!!!!)<br/>
     * spring.sleuth.baggage.remote-fields<br/>
     * keys that also should appear in logging MDC also have to be listed in application.(properties|yml) (!!!!!!!!!)<br/>
     * spring.sleuth.baggage.correlation-fields<br/>
     * They have to also to appear EXACTLY the same in the logging.pattern of application.(properties|yml)
     */
    /* static baggage = stay the same throughout the whole trace (multi-process business flow) */
    BUSINESS_PROCESS_IDS("bpids"), BUSINESS_DOMAIN("ddd"), BUSINESS_PROCESS_NAME("bp")

    /* dynamic baggage = may change throughout the whole trace (multi-process business flow) */
    // none
    ;

    private String key;

    BAGGAGE_REMOTE_KEY(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
