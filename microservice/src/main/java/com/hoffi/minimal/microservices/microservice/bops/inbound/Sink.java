package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SinkChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.ScopedBOP;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TracingHelper tracingHelper;
    
    @StreamListener(SinkChannels.INPUT)
    public void sinked(MessageDTO payload, Message<MessageDTO> wholeMessage) {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = tracingHelper.continueTraceFromUpstream(opName, instanceIndex);

        log.info("continue Trace from upstream: {}", opBOP);

        // explicit call just to show how to alter the current chunkName
        String chunkName = "receive";
        try(ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(opBOP, chunkName, true)) {
            log.info("[{}] FINAL for: '{}'", instanceIndex, payload);
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.tracer().activeSpan().log(t.getMessage()); // Report any errors properly.
        }
        log.info("completely finished trace");
    }

}
