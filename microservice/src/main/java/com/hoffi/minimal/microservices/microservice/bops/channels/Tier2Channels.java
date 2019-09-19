package com.hoffi.minimal.microservices.microservice.bops.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface Tier2Channels {

    String INPUT = "minimal-1To2";
    String OUTPUT = "minimal-2ToSink";

    @Input(INPUT)
    SubscribableChannel tier2Input();

    @Output(OUTPUT)
    MessageChannel tier2Output();
}