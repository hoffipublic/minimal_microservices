package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier2;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
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

    /** for encapsulating as much tracer specifics as possible
     * to keep the business logic (imports) as clean as possible from implementation specif tracing details */
    @Autowired
    private TracingHelper tracingHelper;

    @Autowired
    private SourceTier2 sourceTier2;

    @Monitored("sink2-Receive")
    @StreamListener(Tier2Channels.INPUT)
    public void sinkTier2StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] receive: continue Trace from upstream ...", payload.seq, instanceIndex);

        try {
            log.info("[{} in {}] receive: with upstream span received: '{}'", payload.seq, instanceIndex, payload);

            // as we call this synchronous it will continue THIS CURRENT Span ...
            sourceTier2.sourceTier2SendTo(payload);

            // ALL FOLLOWING will be reported AFTER sources send: operations will have finished
            // as we are calling send synchronously above (stream Processor like)
            // so from here on (viewpoint of Trace) the above called send operations
            // are called in context of this span

            log.info("[{} in {}] receive: finishing receive operation {} ...", payload.seq, instanceIndex, opName);
        } catch (Throwable t) {
            // SpanInScope of Span already has finished at this point
            // also (local) Baggage of parent Span was restored and put into MDC
            log.error(opName, t);
            // Report any errors properly.
            tracingHelper.reportException(t);
        } finally {
            tracingHelper.finishSpanAndOperation(tracingHelper.activeSpan(), opName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("[{} in {}] receive: finished receive operation: {}", payload.seq, instanceIndex, opName);
        }
    }

}
