package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.AverageDurationHelper;
import com.hoffi.minimal.microservices.microservice.tracing.SpanWithBOP;
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
        // wrong place to log as neither the nested Span has started, nor the BOP was initialized
        // log.info("transform beginning for {}", payload);
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = tracingHelper.initBOPfromUpstream(opName, instanceIndex);
        log.info("send: continue Trace from upstream: {}", opBOP);

        Span opSpan = tracingHelper.tracer().nextSpan().name(opName);
        try (SpanWithBOP opSpanWithBOP = tracingHelper.continueTraceFromUpstream(opSpan, opBOP, "send")) {
            BOP spanBOP = opSpanWithBOP.bop();
            log.info("send in Span: {}", spanBOP);

            String chunkNameBusLogic = "businessLogic";
            Span businessLogicSpan = tracingHelper.tracer().nextSpan().name(opName);
            try (SpanWithBOP spanWithBOP_BusinessLogic = tracingHelper.startSpan(businessLogicSpan, opBOP, chunkNameBusLogic)) {
                log.info("transform within {} beginning for {}", chunkNameBusLogic, payload);
                BOP scopeBOP = spanWithBOP_BusinessLogic.bop();

                MessageDTO transformedPayload;
                transformedPayload = businessLogic.firstThingWithinSameSpan(spanWithBOP_BusinessLogic, payload);
                log.info("inbetween firstThing and secondThing");
                transformedPayload = businessLogic.secondThingInNewSpan(spanWithBOP_BusinessLogic, transformedPayload);
                log.info("inbetween secondThink and thirdThing");
                transformedPayload = businessLogic.thirdThingWithNewDynamicBaggage(spanWithBOP_BusinessLogic, transformedPayload, "sink2only");
                log.info("inbetween thirdThing and fourthThing");
                transformedPayload = businessLogic.fourthThingWithScopedBOP(spanWithBOP_BusinessLogic, transformedPayload);
                
                log.info("inbetween fourthThing and notWorkingInner");
                notWorkingInnerThingInANewSpanAndNewDynamicBaggage(transformedPayload);
                log.info("inbetween notWorkingInner and manualSpan");

                String newInnerChunk = "manualSpan";
                Span innerChunkSpan = tracingHelper.tracer().nextSpan().name(newInnerChunk);
                try (SpanWithBOP spanWithBOP_innerManual = tracingHelper.startSpan(innerChunkSpan, spanWithBOP_BusinessLogic.bop(), newInnerChunk)) {
                    log.info("transform manualNewSpan {} start...", newInnerChunk);

                    MessageDTO beforePayload = transformedPayload;
                    transformedPayload = beforePayload.transform(opBOP, "transformed by SourceTier1", "manualNewSpan");
                    log.info("[{}]Transformed from '{}' to '{}'", instanceIndex, beforePayload, transformedPayload);

                    tracingHelper.alterChunk(spanWithBOP_innerManual, "sendWithinManualSpan");

                    log.info("send to next tier within manualNewSpan but new chunk");
                    tier1OutputMessageChannel.send(MessageBuilder.withPayload(transformedPayload).build());
                } catch (Throwable t) {
                    // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
                    // we have to get back its Span into Scope (=active)
                    // chunk will be reported as it was before this try
                    tracingHelper.tracer().withSpanInScope(innerChunkSpan);
                    log.error("Exception on message transform in inner manual Span: {}", t);
                    innerChunkSpan.annotate(t.getMessage()); // Report any errors properly.
                    throw t;
                } finally {
                    tracingHelper.finishSpan(innerChunkSpan, scopeBOP);
                }
                log.info("===after {}", newInnerChunk);
            } catch (Exception e) {
                // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
                // we have to get back its Span into Scope (=active)
                // chunk will be reported as it was before this try
                tracingHelper.tracer().withSpanInScope(businessLogicSpan);
                log.error("Exception on message transform: {}", e);
                businessLogicSpan.annotate(e.getMessage()); // Report any errors properly.
                throw e;
            } finally {
                tracingHelper.finishSpan(businessLogicSpan, spanBOP);
            }

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
            long duration = System.currentTimeMillis() - startTime;
            averageHelper.newAverage(tier, duration);
            log.info("transform ended and took {} ms overall, {}", duration, averageHelper.toString(tier));
            tracingHelper.finishSpanAndOperation(opSpan, opBOP); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished

            log.info("finished operation: {}", opName);
        }

        log.info("========AFTER AFTER 'businessLogic' chunk has finished===========");

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
