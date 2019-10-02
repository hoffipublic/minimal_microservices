package com.hoffi.minimal.microservices.microservice.bops.inbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SinkChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored;
import com.hoffi.minimal.microservices.microservice.tracing.ChunkScoped;
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

@Profile({ "sink" })
@Component
public class Sink {
    private static final Logger log = LoggerFactory.getLogger(Sink.class);

    @Value("${app.info.instance_index}")
    private String instanceIndex;

    /** for encapsulating as much tracer specifics as possible
     * to keep the business logic (imports) as clean as possible from implementation specif tracing details */
    @Autowired
    private TracingHelper tracingHelper;
    
    @Monitored("sink-Receive")
    @StreamListener(SinkChannels.INPUT)
    public void sinked(MessageDTO payload, Message<MessageDTO> wholeMessage) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] receive: continue Trace from upstream ...", payload.seq, instanceIndex);

        String opSpanName = opName+"Span";
        Span opSpan = tracingHelper.nextSpan(opSpanName);
        try (SpanScoped spanScoped = tracingHelper.startSpan(opSpan, opSpanName)) {
            log.info("[{} in {}] final receive: in manualOpSpan {} received '{}'", payload.seq, instanceIndex, opSpanName, payload);

            // explicit call just to show how to alter the current chunkName
            String chunkName = "ThisIsTheEnd";
            try(ChunkScoped chunkScoped = tracingHelper.startChunk(chunkName)) {
                log.info("[{} in {}] receive: FINAL for '{}'", payload.seq, instanceIndex, payload);
            } catch (Throwable t) {
                // SpanInScope of Span already has finished at this point
                // also (local) Baggage of parent Span was restored and put into MDC
                tracingHelper.tracer().withSpanInScope(opSpan);
                log.error("Exception on message transform: {}", t);
                tracingHelper.reportException(opSpan, t);; // Report any errors properly.
            }

            log.info("[{} in {}] receive: TRACE END ... finishing trace in {}", payload.seq, instanceIndex, "final receive");
        } catch (Throwable t) {
            // SpanInScope of Span already has finished at this point
            // also (local) Baggage of parent Span was restored and put into MDC
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(opSpanName, t);
            // Report any errors properly.
            tracingHelper.reportException(opSpan, t);
        } finally {
            log.info("[{} in {}] receive: finishing receive operation's span  {} ...", payload.seq, instanceIndex, opSpanName);
            tracingHelper.finishSpanAndTrace(opSpan, opSpanName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("[{} in {}] receive: completely finished operation and trace in {}", payload.seq, instanceIndex, opName);
        }
    }
}
