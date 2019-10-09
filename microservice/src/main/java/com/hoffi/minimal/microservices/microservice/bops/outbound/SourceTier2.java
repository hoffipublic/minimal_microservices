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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

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

    // publishing to an exchange with no routable queue will never get an exception (only an async return callback)
    @Monitored("sink2-Send")
    @CircuitBreaker(name = "tier2", fallbackMethod = "sourceTier2SendToFallback") // see application.yml
    @SendTo(Tier2Channels.OUTPUT) // redundant here, as we directly use the injected Message Channel
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

    /** fallback method for sourceTier2SendTo (same method signature with added Throwable param) */
    public void sourceTier2SendToFallback(MessageDTO payload, RuntimeException ex) throws Exception {
        log.warn(String.format("FALLBACK for messageDTO %s because of: '%s'", payload.seq, ex.getMessage()));
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
