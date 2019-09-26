package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier2;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
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

@Profile({ "tier2" })
@Component
public class SinkTier2 {
    private static final Logger log = LoggerFactory.getLogger(SinkTier2.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private TracingHelper tracingHelper;

    @Autowired
    private SourceTier2 sourceTier2;

    @StreamListener(Tier2Channels.INPUT)
    public void sinkTier2StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = tracingHelper.initBOPfromUpstream(opName, instanceIndex);
        log.info("receive: continue Trace from upstream: {}", opBOP);

        Span opSpan = tracingHelper.tracer().nextSpan().name(opName);
        try (SpanWithBOP opSpanWithBOP = tracingHelper.continueTraceFromUpstream(opSpan, opBOP, "receive")) {
            BOP spanBOP = opSpanWithBOP.bop();
            log.info("receive: {}", spanBOP);

            log.info("[{}]Received: '{}' with '{}'", instanceIndex, wholeMessage);

            sourceTier2.sourceTier2SendTo(payload);
            log.info("finishing operation {} ...", opName);
        } catch (Throwable t) {
            // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
            // we have to get back its Span into Scope (=active)
            // chunk will be reported as it was before this try
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(opName, t);
            // Report any errors properly.
            opSpan.annotate(String.format("Exception: in operation: %s parentChunk: %s Exception: %s", opBOP.operation, opBOP.chunk, t.getMessage()));
        } finally {
            tracingHelper.finishSpanAndOperation(opSpan, opBOP); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("finished operation: {}", opName);
        }
    }

}
