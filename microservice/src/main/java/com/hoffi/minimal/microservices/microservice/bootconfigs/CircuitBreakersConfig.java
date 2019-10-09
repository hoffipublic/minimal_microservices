package com.hoffi.minimal.microservices.microservice.bootconfigs;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakersConfig {

//     /** spring-cloud-starter-circuitbreaker-resilience4j implementation */
//     @Bean
//     public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
//         TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
//                 .timeoutDuration(Duration.ofMillis(2000l)).cancelRunningFuture(true).build();
//         CircuitBreakerConfig circuitBreakerConfig =
//                 CircuitBreakerConfig.custom().failureRateThreshold(0.33f)
//                         .ringBufferSizeInClosedState(5).ringBufferSizeInHalfOpenState(3)
//                         .waitDurationInOpenState(Duration.ofMillis(1000l)).build();
//         return factory -> factory.configureDefault(
//                 id -> new Resilience4JConfigBuilder(id).timeLimiterConfig(timeLimiterConfig)
//                         .circuitBreakerConfig(circuitBreakerConfig).build());
//     }

        // all resilience4j configs are done in application.yml
        // @Autowired
        // CircuitBreakerRegistry circuitBreakerRegistry;



}