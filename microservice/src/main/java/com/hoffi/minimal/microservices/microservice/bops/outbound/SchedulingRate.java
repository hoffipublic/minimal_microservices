package com.hoffi.minimal.microservices.microservice.bops.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
public class SchedulingRate {

    private static final Logger log = LoggerFactory.getLogger(SchedulingRate.class);

    @Value("${app.sources.fixedDelay}")
    private long fixedDelay;

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        log.info("setting SchedulingRate to {}", fixedDelay);
        this.fixedDelay = fixedDelay;
    }
}
