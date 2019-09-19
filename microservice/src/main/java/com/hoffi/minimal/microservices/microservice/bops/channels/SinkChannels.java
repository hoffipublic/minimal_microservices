package com.hoffi.minimal.microservices.microservice.bops.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface SinkChannels {

    String INPUT = "minimal-2ToSink";

    @Input(INPUT)
    SubscribableChannel sinkInput();
}