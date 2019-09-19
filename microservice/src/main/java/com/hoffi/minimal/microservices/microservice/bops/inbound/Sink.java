package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SinkChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.MDCLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Profile({ "sink" })
@Component
public class Sink {
    private static final Logger log = LoggerFactory.getLogger(Sink.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    // @Autowired
    // private CustomBaggage customBaggage;

    @StreamListener(SinkChannels.INPUT)
    //@NewSpan("SinkStreamListener")
    public void sinked(MessageDTO payload, Message<MessageDTO> wholeMessage) {
        String newChunk = new Object() {}.getClass().getEnclosingMethod().getName();
        MDCLocal.startChunk(newChunk);
        try {
            //        log.info("[{}]Received: '{}' wholeMessage '{}'", instanceIndex, payload, wholeMessage);
            log.info("[{}]Received: '{}'", instanceIndex, wholeMessage);
            // we now are the final step for this business process,
            // so we also tag with a special tag indicating exactly this
            // if you want this change to be propagated to zipkin, you have to start a new tagged span
            // otherwise the new Baggage will only be logged
            // and submitted downstream of course
           // customBaggage.dynBaggageTagSuccessorProcess("FINAL");
            log.info("[{}] FINAL for: '{}'", instanceIndex, payload);
        } finally {
            MDCLocal.endChunk(newChunk);
        }
    }

}
