package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier1;
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

@Profile({ "tier1" })
@Component
public class SinkTier1 {
    private static final Logger log = LoggerFactory.getLogger(SinkTier1.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    // @Autowired
    // private CustomBaggage customBaggage;

    @Autowired
    private SourceTier1 sourceTier1;

    @StreamListener(Tier1Channels.INPUT)
    //@NewSpan("SinkTier1StreamListener")
    // public void transform(MessageDTO payload, @Header("baggage-bpn") String bpn, @Header("baggage-succ") String succ, Message<MessageDTO> wholeMessage) throws Exception {
    public void sinkTier1StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        MDCLocal.startChunk(new Object() {}.getClass().getEnclosingMethod().getName());
        try {
            //        log.info("[{}]Received: '{}' wholeMessage '{}'", instanceIndex, payload, wholeMessage);
            log.info("[{}]Received: '{}'", instanceIndex, wholeMessage);
            // first thing after receiving is, that we know which possible downstream services might be called
            // if you want this change to be propagated to zipkin, you have to start a new tagged span
            // otherwise the new Baggage will only be logged (and submitted downstream of course)
            // customBaggage.dynBaggageTagSuccessorProcess("sink2|evenOther");
            log.info("[{}] set NEW downstream(s) for: '{}'", instanceIndex, payload);

            sourceTier1.sourceTier1SendTo(payload);
        } finally {
            MDCLocal.endChunk(new Object() {}.getClass().getEnclosingMethod().getName());
        }
    }

}
