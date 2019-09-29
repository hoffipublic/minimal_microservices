package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.AverageDurationHelper;
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

    // @Autowired
    // private CustomBaggage customBaggage;
    // @Autowired
    // private Zipkin zipkin;

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

    /*
     * When calls to a particular service exceed circuitBreaker.requestVolumeThreshold (default: 20
     * requests) and the failure percentage is greater than circuitBreaker.errorThresholdPercentage
     * (default: >50%) in a rolling window defined by metrics.rollingStats.timeInMilliseconds
     * (default: 10 seconds), the circuit opens and the call is not made. In cases of error and an
     * open circuit, a fallback can be provided by the developer.
     */

    // publishing to an exchange with no routable queue will never get an exception (only an async
    // return callback)
    // @formatter:off
    @SendTo(Tier1Channels.OUTPUT) // redundant here, as we directly use the injected Message Channel
    // @HystrixCommand(fallbackMethod = "fallbackTimerMessageSource", commandProperties = {
    //         @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "20000"), // time that the circuitbreakes "remembers" calls to this circuitbreaker
    //         @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "3"), // to trip open, more than this calls have to come in within the metrics.rollingStats.timeInMilliseconds
    //         @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60"), //the error percentage at or above which the circuit should trip open and start short-circuiting requests to fallback logic
    //         @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"), // if opened, time how long will be open in any case
    //         @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000") // time after which the fallback method willl be called anyway
    // })
    // @formatter:on
    public void sourceTier1SendTo(MessageDTO payload) throws Exception {
        long startTime = System.currentTimeMillis();
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] send: continue Trace from upstream: {}", payload.seq, instanceIndex, opName);

        // just for fun, we add an outer Span
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

    // @NewSpan("SourceTier1SendToFallback")
    // public void fallbackTimerMessageSource(MessageDTO payload, Throwable t) {
    // if (t instanceof HystrixTimeoutException) {
    // log.error("Hystrix fallback. The BusinessLogic exceeded the
    // execution.isolation.thread.timeoutInMilliseconds of 3000ms");
    // } else {
    // log.error("Hystrix fallback. Message '{}' couldn't be transformed. The original exception
    // was", payload, t);
    // }
    // }


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
