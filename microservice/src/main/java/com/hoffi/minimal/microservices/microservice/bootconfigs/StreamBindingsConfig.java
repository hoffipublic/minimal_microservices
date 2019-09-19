package com.hoffi.minimal.microservices.microservice.bootconfigs;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hoffi.minimal.microservices.microservice.bops.channels.SinkChannels;
import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;

@Configuration
public class StreamBindingsConfig {

    @Profile({ "source" })
    @EnableBinding(SourceChannels.class)
    static class StreamBindingFromSourceTo1Config {

    }

    @Profile({ "tier1" })
    @EnableBinding(Tier1Channels.class)
    static class StreamBindingFrom1to2Config {

    }

    @Profile({ "tier2" })
    @EnableBinding(Tier2Channels.class)
    static class StreamBindingFrom2To3Config {

    }

    @Profile({ "sink" })
    @EnableBinding(SinkChannels.class)
    static class StreamBindingFrom3ToSinkConfig {

    }
}
