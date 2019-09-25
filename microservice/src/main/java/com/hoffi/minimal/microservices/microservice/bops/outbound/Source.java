package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.ScopedChunk;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.TracingHelper;
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
import io.opentracing.Span;

@Profile({ "source" })
@Component
public class Source {
    private static Logger log = LoggerFactory.getLogger(Source.class);
    
    @Value("${app.businessLogic.tier}")
    private String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;

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

    // publishing to an exchange with no routable queue will never get an exception (only an async return callback)
    // @Scheduled(fixedDelay = 2500, initialDelay = 500) // commented out in favour of the RefreshableScheduler in this same package
    //@HystrixCommand(fallbackMethod = "fallbackTimerMessageSource")
    @ImplementationHint(clazz = RefreshableScheduler.class)
    @SendTo(SourceChannels.OUTPUT) // redundant here, as we directly use the injected Message Channel
    public void timerMessageSource() {
        // defining a new Business-Process and starting a downstream Trace for it
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        BOP opBOP = BOP.initInitially("myCurrentBDomain", "myBProcessName", opName, instanceIndex); // autogenerated BusinessProcess-ID
        // ... start a new Trace
        Span bopSpan = tracingHelper.tracer().buildSpan(opName).start();
        try (ScopedChunk scopedChunk = tracingHelper.startTrace(bopSpan, opBOP, "timerSend", true)) {
            BOP scopeBOP = scopedChunk.bop();
            MessageDTO messageDTO = MessageDTO.create(scopeBOP);
            messageDTO.message = "fromSource";

            log.info("[{}]Produced: '{}'", instanceIndex, messageDTO);
            sourceOutputMessageChannel.send(MessageBuilder.withPayload(messageDTO).build());
        } catch (Exception e) {
            bopSpan.log(e.getMessage()); // Report any errors properly.
        } finally {
            log.info("{} END", opName);
        }
        log.info("CHECK if chunk is 'default' again");
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
