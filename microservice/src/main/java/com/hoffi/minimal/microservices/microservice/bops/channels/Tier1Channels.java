package com.hoffi.minimal.microservices.microservice.bops.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface Tier1Channels {

    String INPUT = "minimal-SourceTo1";
    String OUTPUT = "minimal-1To2";

    @Input(INPUT)
    SubscribableChannel tier1Input();

    @Output(OUTPUT)
    MessageChannel tier1Output();
}