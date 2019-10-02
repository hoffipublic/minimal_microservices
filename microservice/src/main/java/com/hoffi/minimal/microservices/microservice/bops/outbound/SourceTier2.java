package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;
import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.AverageDurationHelper;
import com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored;
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

@Profile({ "tier2" })
@Component
public class SourceTier2 {

    private static final Logger log = LoggerFactory.getLogger(SourceTier2.class);

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
    //    @Autowired
    //    private Tier2Channels tier2Channels;
    @Autowired
    @Qualifier(Tier2Channels.OUTPUT)
    private MessageChannel tier2OutputMessageChannel;

    @Autowired
    private BusinessLogic businessLogic;

    /*
     * When calls to a particular service exceed circuitBreaker.requestVolumeThreshold (default: 20 requests) and the
     * failure percentage is greater than circuitBreaker.errorThresholdPercentage (default: >50%) in a rolling window
     * defined by metrics.rollingStats.timeInMilliseconds (default: 10 seconds), the circuit opens and the call is not made.
     * In cases of error and an open circuit, a fallback can be provided by the developer.
     */

    // publishing to an exchange with no routable queue will never get an exception (only an async return callback)
    // @formatter:off
    @Monitored("sink2-Send")
    @SendTo(Tier2Channels.OUTPUT) // redundant here, as we directly use the injected Message Channel
    // @HystrixCommand(fallbackMethod = "fallbackTimerMessageSource", commandProperties = {
    //         @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "20000"), // time that the circuitbreakes "remembers" calls to this circuitbreaker
    //         @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "3"), // to trip open, more than this calls have to come in within the metrics.rollingStats.timeInMilliseconds
    //         @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60"), //the error percentage at or above which the circuit should trip open and start short-circuiting requests to fallback logic
    //         @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"), // if opened, time how long will be open in any case
    //         @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000") // time after which the fallback method willl be called anyway
    // })
    // @NewSpan("SourceTier2SendTo")
    // @formatter:on
    public void sourceTier2SendTo(MessageDTO payload) throws Exception {
        long startTime = System.currentTimeMillis();
        String opName = new Object() {}.getClass().getEnclosingMethod().getName();
        // setting local baggage keys, tags and annotations on the incoming Span
        tracingHelper.continueTraceFromUpstream(opName, instanceIndex);
        log.info("[{} in {}] send: continue Trace from upstream...", payload.seq, instanceIndex);

        try {
            log.info("[{} in {}] send: transform using upstream span beginning for {}", payload.seq, instanceIndex, payload);

            MessageDTO transformedPayload;
            transformedPayload = businessLogic.firstThingWithinSameSpan(payload);
            log.info("[{} in {}] send: inbetween firstThing and secondThing", payload.seq, instanceIndex);
            transformedPayload = businessLogic.secondThingInNewSpan(transformedPayload);
            log.info("[{} in {}] send: inbetween secondThink and thirdThing", payload.seq, instanceIndex);
            transformedPayload = businessLogic.thirdThingWithNewDynamicBaggage(transformedPayload, "sink");
            log.info("[{} in {}] send: inbetween thirdThing and fourthThing", payload.seq, instanceIndex);
            transformedPayload = businessLogic.fourthThingWithScopedBOP(transformedPayload);

            log.info("[{} in {}] send: send to next tier within upstream span: {}", payload.seq, instanceIndex, opName);
            tier2OutputMessageChannel.send(MessageBuilder.withPayload(transformedPayload).build());

            log.info("[{} in {}] send: finishing span {} (after async downstream send) ...", payload.seq, instanceIndex, opName);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            averageHelper.newAverage(tier, duration);
            log.info("[{} in {}] send: transform ended and took {} ms overall, {}", payload.seq, instanceIndex, duration, averageHelper.toString(tier));
            log.info("[{} in {}] send: finishing operation's span  {} ...", payload.seq, instanceIndex, opName);
            // as for this demo this method was called synchonously and NOT VIA MESSAGING
            // we shouldn't finish the continueTraceFromUpstream Span,
            // as it will continue in the inbound.SinkTier1 receiver ...
            // tracingHelper.finishSpanAndOperation(tracingHelper.activeSpan(), opName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished

            log.info("[{} in {}] send: finished send operation: {}", payload.seq, instanceIndex, opName);
        }
        
        log.info("[{} in {}] send: ========AFTER AFTER 'businessLogic' has finished===========", payload.seq, instanceIndex);
    }

    // @NewSpan("SourceTier2SendToFallback")
    // public void fallbackTimerMessageSource(MessageDTO payload, Throwable t) {
    //     if (t instanceof HystrixTimeoutException) {
    //         log.error("Hystrix fallback. The BusinessLogic exceeded the execution.isolation.thread.timeoutInMilliseconds of 3000ms");
    //     } else {
    //         log.error("Hystrix fallback. Message '{}' couldn't be transformed. The original exception was", payload, t);
    //     }
    // }
}
