package com.hoffi.minimal.microservices.microservice.monitoring.aspects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;

@Component
public class AllMetersMap {
    private static final Logger log = LoggerFactory.getLogger(AllMetersMap.class);

    public Map<String, Meter> metersMap = new ConcurrentHashMap<>();

    public Counter getCounter(String name) {
        return (Counter) metersMap.get(name);
    }

    public Gauge getGauge(String name) {
        return (Gauge) metersMap.get(name);
    }

    public LongTaskTimer getLongTaskTimer(String name) {
        return (LongTaskTimer) metersMap.get(name);
    }

    public Timer getTimer(String name) {
        return (Timer) metersMap.get(name);
    }

    public void putCounter(String name, Counter counter) {
        log.info(this.getClass().getSimpleName() + " defining counter: " + name);
        if(metersMap.putIfAbsent(name, counter) != null) {
            throw new RuntimeException(String.format("Counter '%s' does already exist or already was initialised", name));
        }
    }

    public void putGauge(String name, Gauge gauge) {
        log.info(this.getClass().getSimpleName() + " defining gauge: " + name);
        if(metersMap.putIfAbsent(name, gauge) != null) {
            throw new RuntimeException(String.format("Gauge '%s' does already exist or already was initialised", name));
        }
    }

    public void putTimer(String name, Timer timer) {
        log.info(this.getClass().getSimpleName() + " defining timer: " + name);
        if(metersMap.putIfAbsent(name, timer) != null) {
            throw new RuntimeException(String.format("Timer '%s' does already exist or already was initialised", name));
        }
    }
}