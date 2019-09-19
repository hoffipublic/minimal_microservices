package com.hoffi.minimal.microservices.microservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
public class MicroserviceApplication implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MicroserviceApplication.class);

    @Value("${my.test.property}")
    String myTestProperty;

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("myTestProperty: {}", myTestProperty);
    }
}
