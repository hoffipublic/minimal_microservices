package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
import com.hoffi.minimal.microservices.microservice.helpers.SeqNr;
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

@Profile({ "source" })
@Component
public class Source {
    private static Logger log = LoggerFactory.getLogger(Source.class);
    
    @Value("${app.businessLogic.tier}")
    private String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;

    /** for encapsulating as much tracer specifics as possible
     * to keep the business logic (imports) as clean as possible from implementation specif tracing details */
    @Autowired
    private TracingHelper tracingHelper;
    
    // as both: injecting the Channel and only @SendTo annotation on the producer-method
    // let the send happen where the @HystrixCommand cannot catch and extract
    // we have to directly inject and use the MessageChannel on producing messages
    //    @Autowired
    //    private SourceChannels sourceChannels;
    @Autowired
    @Qualifier(SourceChannels.OUTPUT)
    private MessageChannel sourceOutputMessageChannel;

    public static final String BPDOM = "myBPDomain";
    public static final String BPROC = "myBProcess";

    // publishing to an exchange with no routable queue will never get an exception (only an async return callback)
    // @Scheduled(fixedDelay = 2500, initialDelay = 500) // commented out in favour of the RefreshableScheduler in this same package
    @ImplementationHint(clazz = RefreshableScheduler.class)
    @Monitored("source-Send")
    @CircuitBreaker(name = "source", fallbackMethod = "timerMessageSourceFallback") // see application.yml
    @SendTo(SourceChannels.OUTPUT) // redundant here, as we directly use the injected Message Channel
    public void timerMessageSource() throws Exception {
        // defining a new Business-Process and starting a downstream Trace for it
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name

        // ... start a completely new Trace
        Span opSpan = tracingHelper.newTrace(opName);
        try (SpanScoped spanScoped = tracingHelper.startTrace(opSpan, BPDOM, BPROC, SeqNr.nextBPId(), opName, instanceIndex)) {

            MessageDTO messageDTO = MessageDTO.create("fromSource");
            
            log.info("[{} in {}] Producing: '{}'", messageDTO.seq, instanceIndex, messageDTO);
            // if (true) { throw new Exception("intentional Exception before send"); }

            sourceOutputMessageChannel.send(MessageBuilder.withPayload(messageDTO).build());
            
            log.info("[{} in {}] finishing operation {} (after async downstream send) ...", messageDTO.seq, instanceIndex, opName);
        } catch (Exception e) {
            // SpanInScope of Span already has finished at this point
            // also (local) Baggage of parent Span was restored and put into MDC
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(opName, e);
            // Report any errors properly.
            tracingHelper.reportException(opSpan, e);
            throw e;
        } finally {
            log.info("finishing initial's Trace operation's span {} ...", opName);
            tracingHelper.finishSpanAndOperation(opSpan, opName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
            log.info("AFTER INITIAL's Trace operation --> CHECK if BusinessProcess info in logging whatsoever");
        }
    }

    /** fallback method for timerMessageSource (same method signature with added Throwable param) */
    public void timerMessageSourceFallback(RuntimeException ex) throws Exception {
        log.warn(String.format("FALLBACK because of: '%s'", ex.getMessage()));
    }


    // @NewSpan("SourceSendToFallback")
    // public void fallbackTimerMessageSource(Throwable t) {
    //     if (t instanceof HystrixTimeoutException) {
    //         log.error("Hystrix fallback. The BusinessLogic exceeded the execution.isolation.thread.timeoutInMilliseconds");
    //     } else {
    //         log.error("Hystrix fallback. Message couldn't be send. The original exception was", t);
    //     }
    // }
}
