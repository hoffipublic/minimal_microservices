package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier2;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.MDCLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Profile({ "tier2" })
@Component
public class SinkTier2 {
    private static final Logger log = LoggerFactory.getLogger(SinkTier2.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    // @Autowired
    // private CustomBaggage customBaggage;

    @Autowired
    private SourceTier2 sourceTier2;

    @StreamListener(Tier2Channels.INPUT)
    //@NewSpan("SinkTier2StreamListener")
    public void sinkTier2StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String newChunk = new Object() {}.getClass().getEnclosingMethod().getName();
        MDCLocal.startChunk(newChunk);
        try {
            //        log.info("[{}]Received: '{}' with '{}' wholeMessage '{}'", instanceIndex, payload, wholeMessage);
            log.info("[{}]Received: '{}' with '{}'", instanceIndex, wholeMessage);
            // first thing after receiving is, that we know which possible downstream services might be called
            // if you want this change to be propagated to zipkin, you have to start a new tagged span
            // otherwise the new Baggage will only be logged
            // and submitted downstream of course
            // customBaggage.dynBaggageTagSuccessorProcess("sinkFinal|derivation");
            log.info("[{}] set NEW downstream(s) for: '{}'", instanceIndex, payload);

            sourceTier2.sourceTier2SendTo(payload);
        } finally {
            MDCLocal.endChunk(newChunk);
        }
    }

}
