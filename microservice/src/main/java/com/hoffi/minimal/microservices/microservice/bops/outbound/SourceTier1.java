package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.AverageDurationHelper;
import com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored;
import com.hoffi.minimal.microservices.microservice.tracing.SpanScoped;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import brave.Span;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Profile({"tier1"})
@Component
public class SourceTier1 {
    private static final Logger log = LoggerFactory.getLogger(SourceTier1.class);

    @Value("${app.businessLogic.tier}")
    private String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private AverageDurationHelper averageHelper;

    /** for encapsulating as much tracer specifics as possible
     * to keep the business logic (imports) as clean as possible from implementation specif tracing details */
    @Autowired
    private TracingHelper tracingHelper;

    // as both: injecting the Channel and only @SendTo annotation on the producer-method
    // let the send happen where the @HystrixCommand cannot catch and extract
    // we have to directly inject and use the MessageChannel on producing
    // @Autowired
    // private Tier1Channels tier1Channels;
    @Autowired
    @Qualifier(Tier1Channels.OUTPUT)
    private MessageChannel tier1OutputMessageChannel;

    @Autowired
    private BusinessLogic businessLogic;

    // publishing to an exchange with no routable queue will never get an exception (only an async return callback)
    @Monitored("sink1-Send")
    @CircuitBreaker(name = "tier1", fallbackMethod = "sourceTier1SendToFallback") // see application.yml
    @SendTo(Tier1Channels.OUTPUT) // redundant here, as we directly use the injected Message Channel
    public void sourceTier1SendTo(MessageDTO payload) throws Exception {
        long startTime = System.currentTimeMillis();
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] send: continue Trace from upstream: {}", payload.seq, instanceIndex, opName);

        String opSpanName = opName+"Span";
        Span outerSpan = tracingHelper.nextSpan(opSpanName);
        try (SpanScoped spanScoped = tracingHelper.startSpan(outerSpan, opSpanName)) {
            log.info("[{} in {}] send: in manualOpSpan {}", payload.seq, instanceIndex, opSpanName);

            String chunkNameBusLogic = "businessLogic";
            Span businessLogicSpan = tracingHelper.nextSpan(chunkNameBusLogic);
            try (SpanScoped businessLogicSpanScoped = tracingHelper.startSpan(businessLogicSpan, chunkNameBusLogic)) {
                log.info("[{} in {}] send: transform within new span {} beginning for {}", payload.seq, instanceIndex, chunkNameBusLogic, payload);

                MessageDTO transformedPayload;
                transformedPayload = businessLogic.firstThingWithinSameSpan(payload);
                log.info("[{} in {}] send: inbetween firstThing and secondThing", payload.seq, instanceIndex);
                transformedPayload = businessLogic.secondThingInNewSpan(transformedPayload);
                log.info("[{} in {}] send: inbetween secondThink and thirdThing", payload.seq, instanceIndex);
                transformedPayload = businessLogic.thirdThingWithNewDynamicBaggage(transformedPayload, "sink2only");
                log.info("[{} in {}] send: inbetween thirdThing and fourthThing", payload.seq, instanceIndex);
                transformedPayload = businessLogic.fourthThingWithScopedBOP(transformedPayload);
                
                log.info("[{} in {}] send: inbetween fourthThing and notWorkingInner", payload.seq, instanceIndex);
                notWorkingInnerThingInANewSpanAndNewDynamicBaggage(transformedPayload);
                log.info("[{} in {}] send: inbetween notWorkingInner and manualSpan", payload.seq, instanceIndex);

                String newInnerChunk = "manualSpan";
                Span innerChunkSpan = tracingHelper.nextSpan(newInnerChunk);
                try (SpanScoped manualSpanSpanScoped = tracingHelper.startSpan(innerChunkSpan, newInnerChunk)) {
                    log.info("[{} in {}] send: transform within manualNewSpan {} start...", payload.seq, instanceIndex, newInnerChunk);

                    MessageDTO beforePayload = transformedPayload;
                    transformedPayload = beforePayload.transform("transformed by " + newInnerChunk, newInnerChunk);
                    log.info("[{} in {}] send: transformed from to '{}'", payload.seq, instanceIndex, transformedPayload);

                    tracingHelper.alterChunk(spanScoped, "sendWithinManualSpan");

                    log.info("[{} in {}] send: send to next tier within manualNewSpan but new chunk: sendWithinManualSpan", payload.seq, instanceIndex);
                    tier1OutputMessageChannel.send(MessageBuilder.withPayload(transformedPayload).build());
                    log.info("[{} in {}] send: finishing span {} (after async downstream send) ...", payload.seq, instanceIndex, newInnerChunk);
                } catch (Throwable t) {
                    // SpanInScope of Span already has finished at this point
                    // also (local) Baggage of parent Span was restored and put into MDC
                    tracingHelper.tracer().withSpanInScope(innerChunkSpan);
                    log.error("Exception on message transform in inner manual Span: {}", t);
                    tracingHelper.reportException(innerChunkSpan, t); // Report any errors properly.
                    throw t;
                } finally {
                    log.info("[{} in {}] send: finishing span {} ==> now finishing {} ...", payload.seq, instanceIndex, newInnerChunk, chunkNameBusLogic);
                    tracingHelper.finishSpan(innerChunkSpan);
                }

                log.info("[{} in {}] send: after span {}", payload.seq, instanceIndex, newInnerChunk);
            } catch (Exception e) {
                // SpanInScope of Span already has finished at this point
                // also (local) Baggage of parent Span was restored and put into MDC
                tracingHelper.tracer().withSpanInScope(businessLogicSpan);
                log.error("Exception on message transform: {}", e);
                tracingHelper.reportException(businessLogicSpan, e); // Report any errors properly.
                throw e;
            } finally {
                log.info("[{} in {}] send: finishing span {} ...", payload.seq, instanceIndex, chunkNameBusLogic);
                tracingHelper.finishSpan(businessLogicSpan);
            }

            log.info("[{} in {}] send: finished span {} ==> now finishing {} ...", payload.seq, instanceIndex, chunkNameBusLogic, opSpanName);
        } catch (Throwable t) {
            // SpanInScope of Span already has finished at this point
            // also (local) Baggage of parent Span was restored and put into MDC
            tracingHelper.tracer().withSpanInScope(outerSpan);
            log.error(opName, t);
            // Report any errors properly.
            tracingHelper.reportException(outerSpan, t);
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            averageHelper.newAverage(tier, duration);
            log.info("[{} in {}] send: transform ended and took {} ms overall, {}", payload.seq, instanceIndex, duration, averageHelper.toString(tier));
            log.info("[{} in {}] send: finishing send operation's span  {} ...", payload.seq, instanceIndex, opSpanName);
            // as for this demo this method was called synchonously and NOT VIA MESSAGING
            // we shouldn't finish the continueTraceFromUpstream Span,
            // as it will continue in the inbound.SinkTier1 receiver ...
            // tracingHelper.finishSpanAndOperation(tracingHelper.activeSpan(), opName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished

            log.info("[{} in {}] send: finished span of send operation: {}", payload.seq, instanceIndex, opName);
        }

        log.info("[{} in {}] send: ========AFTER AFTER 'businessLogic' chunk has finished===========", payload.seq, instanceIndex);
    }

    /** fallback method for sourceTier1SendTo (same method signature with added Throwable param) */
    public void sourceTier1SendToFallback(MessageDTO payload, RuntimeException ex) throws Exception {
        log.warn(String.format("FALLBACK for messageDTO %s because of: '%s'", payload.seq, ex.getMessage()));
    }

    /**
     * if called from the same Bean/class @NewSpan is *not* able to intercept the call and set the
     * new span
     */
    // @NewSpan("notWorking")
    public MessageDTO notWorkingInnerThingInANewSpanAndNewDynamicBaggage(MessageDTO payload)
            throws Exception {
        // ==================================================================
        // this does NOT create a new span if it is called from the same bean
        // (this.notWorkingInnerThingInANewSpanAndNewDynamicBaggage)
        // ==================================================================
        String bop = "notWorkingInnerThingInANewSpanAndNewDynamicBaggage";
        log.info("BusinessOperation.{} START", bop);
        // so maybe at this point we know more specific which downstream calls are possible
        // customBaggage.dynBaggageTagSuccessorProcess("noNewSpan");
        // if you want this change to be propagated to zipkin, you have to start a new tagged span
        // as we will do now ...
        log.info("BusinessOperation.{} new dynamic Baggage", bop);

        Thread.sleep(20);

        log.info("BusinessOperation.{} END", bop);

        return payload;
    }
}
