package com.hoffi.minimal.microservices.microservice.bops.outbound;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Profile({"source"})
@Configuration
public class RefreshableScheduler implements SchedulingConfigurer {
    private static Logger log = LoggerFactory.getLogger(RefreshableScheduler.class);

    @Autowired
    private SchedulingRate schedulingRate;

    @Autowired
    private Source source;

    @Autowired
    Environment springEnv;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
        taskRegistrar.addTriggerTask(() -> {
            try {
                source.timerMessageSource(); // this will be called
            } catch (Exception e) {
                log.error("Exception on calling source.timerMessageSource() in RefreshableScheduler", e);
            }
        }, triggerContext -> {
            if (springEnv.acceptsProfiles(Profiles.of("unscheduled"))) {
                return null;
            } else if (springEnv.acceptsProfiles(Profiles.of("once"))) {
                if (triggerContext.lastActualExecutionTime() == null) {
                    return new Date();
                }
                return null;
            } else {
                Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
                long lastActualExecutionTimeInstant = (lastActualExecutionTime != null ? lastActualExecutionTime.getTime()
                        : System.currentTimeMillis());
                Date nextExecutionTime = new java.util.Date(lastActualExecutionTimeInstant + schedulingRate.getFixedDelay());
                return nextExecutionTime;
            }
        });
    }

    // ensure that the task executor is properly shut down when the Spring application context itself is closed
    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(42);
    }
}
