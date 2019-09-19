package com.hoffi.minimal.microservices.microservice.bops.channels;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface SourceChannels {

    String OUTPUT = "minimal-SourceTo1";

    @Output(OUTPUT)
    MessageChannel sourceOutput();
}