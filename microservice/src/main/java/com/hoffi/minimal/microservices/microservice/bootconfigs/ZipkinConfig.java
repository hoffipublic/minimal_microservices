package com.hoffi.minimal.microservices.microservice.bootconfigs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

@Configuration
public class ZipkinConfig {

    @Profile("test")
    @Bean
    Reporter<Span> reporter() {
        return Reporter.NOOP;
    }

}
