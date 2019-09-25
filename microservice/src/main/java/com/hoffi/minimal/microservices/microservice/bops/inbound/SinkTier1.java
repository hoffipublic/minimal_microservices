package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier1;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Profile({"tier1"})
@Component
public class SinkTier1 {
    private static final Logger log = LoggerFactory.getLogger(SinkTier1.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private TracingHelper tracingHelper;

    @Autowired
    private SourceTier1 sourceTier1;

    /** instrumentation has started a new Span for us */
    @StreamListener(Tier1Channels.INPUT)
    public void sinkTier1StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage)
            throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("receive: continue Trace from upstream: {}", opBOP);

        // log.info("[{}]Received: '{}' wholeMessage '{}'", instanceIndex, payload, wholeMessage);
        log.info("[{}]Received: '{}'", instanceIndex, wholeMessage);

        sourceTier1.sourceTier1SendTo(payload);
    }

}
