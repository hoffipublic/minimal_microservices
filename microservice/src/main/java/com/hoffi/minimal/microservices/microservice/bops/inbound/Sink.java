package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SinkChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.tracing.ScopedBOP;
import com.hoffi.minimal.microservices.microservice.tracing.SpanWithBOP;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import brave.Span;

@Profile({ "sink" })
@Component
public class Sink {
    private static final Logger log = LoggerFactory.getLogger(Sink.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private TracingHelper tracingHelper;
    
    @StreamListener(SinkChannels.INPUT)
    public void sinked(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = tracingHelper.initBOPfromUpstream(opName, instanceIndex);
        log.info("receive: continue Trace from upstream: {}", opBOP);

        Span opSpan = tracingHelper.tracer().nextSpan().name(opName);
        try (SpanWithBOP opSpanWithBOP = tracingHelper.continueTraceFromUpstream(opSpan, opBOP, "receive")) {
            BOP spanBOP = opSpanWithBOP.bop();
            log.info("receive: continue Trace from upstream: {}", spanBOP);

            // explicit call just to show how to alter the current chunkName
            String chunkName = "finallyInUnscpedChunk";
            try(ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(opBOP, chunkName)) {
                log.info("[{}] FINAL for: '{}'", instanceIndex, payload);
            } catch (Throwable t) {
                log.error("Exception on message transform: {}", t);
                opSpan.annotate(t.getMessage()); // Report any errors properly.
            }

            log.info("TRACE END ... finishing trace in {}", opName);
        } catch (Throwable t) {
            // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
            // we have to get back its Span into Scope (=active)
            // chunk will be reported as it was before this try
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(opName, t);
            // Report any errors properly.
            opSpan.annotate(String.format("Exception: in operation: %s parentChunk: %s Exception: %s", opBOP.operation, opBOP.chunk, t.getMessage()));
        } finally {
            tracingHelper.finishSpanAndTrace(opSpan, opBOP); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("completely finished operation and trace in {}", opName);
        }
    }
}
