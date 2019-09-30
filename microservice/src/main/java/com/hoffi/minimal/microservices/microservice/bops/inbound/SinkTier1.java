package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier1;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.tracing.SpanScoped;
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
import io.micrometer.core.annotation.Timed;

@Profile({"tier1"})
@Component
public class SinkTier1 {
    private static final Logger log = LoggerFactory.getLogger(SinkTier1.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    /** for encapsulating as much tracer specifics as possible
     * to keep the business logic (imports) as clean as possible from implementation specif tracing details */
    @Autowired
    private TracingHelper tracingHelper;

    @Autowired
    private SourceTier1 sourceTier1;

    @Timed("sinkTier1StreamListenerMetrics")
    @StreamListener(Tier1Channels.INPUT)
    public void sinkTier1StreamListener(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] receive: continue Trace from upstream ...", payload.seq, instanceIndex);

        String opSpanName = opName+"Span";
        Span opSpan = tracingHelper.nextSpan(opSpanName);
        try (SpanScoped outer = tracingHelper.startSpan(opSpan, opSpanName)) {
            //log.info("[{} in {}] receive: in newSpan {} received '{}'", payload.seq, instanceIndex, opSpanName, wholeMessage);
            log.info("[{} in {}] receive: in manualOpSpan {} received '{}'", payload.seq, instanceIndex, opSpanName, payload);
            
            tracingHelper.alterChunk(outer, "Tier1SendWithNewChunkName");
            log.info("[{} in {}] receive: in manualOpSpan {} altered chunk name", payload.seq, instanceIndex, opSpanName);

            // as we call this synchronous it will continue THIS CURRENT Span ...
            sourceTier1.sourceTier1SendTo(payload);

            // ALL FOLLOWING will be reported AFTER sources send: operations will have finished
            // as we are calling send synchronously above (stream Processor like)
            // so from here on (viewpoint of Trace) the above called send operations
            // are called in context of this span

            log.info("[{} in {}] receive: finishing manualOpSpan {}  ...", payload.seq, instanceIndex, opSpanName);
        } catch (Throwable t) {
            // SpanInScope of Span already has finished at this point
            // also (local) Baggage of parent Span was restored and put into MDC
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(opSpanName, t);
            // Report any errors properly.
            tracingHelper.reportException(t);
        } finally {
            log.info("[{} in {}] receive: finishing receive operation's span  {} ...", payload.seq, instanceIndex, opSpanName);
            tracingHelper.finishSpanAndOperation(opSpan, opSpanName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("[{} in {}] receive: finished receive operation: {}", payload.seq, instanceIndex, opSpanName);
        }
    }

}
