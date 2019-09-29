package com.hoffi.minimal.microservices.microservice.businesslogic;

import java.util.Random;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.tracing.ChunkScoped;
import com.hoffi.minimal.microservices.microservice.tracing.SpanScoped;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import brave.Span;

/**
 *
 * @author hoffmd9
 *
 */
@Component
public class BusinessLogic {
    private static Logger log = LoggerFactory.getLogger(BusinessLogic.class);

    @Value("${app.businessLogic.tier}")
    public String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;
    @Value("${app.businessLogic.sleepMin}")
    public int sleepMin;
    @Value("${app.businessLogic.sleepMax}")
    public int sleepMax;

    @Autowired
    private TracingHelper tracingHelper;

    public BusinessLogic() {
    }

    public MessageDTO firstThingWithinSameSpan(MessageDTO payload) throws BusinessException {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name

        try(ChunkScoped chunkScoped = tracingHelper.startChunk(opName)) {
            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic(payload.seq);
            MessageDTO transformedPayload = payload.transform("firstThing Transformation", opName);
            
            return transformedPayload;

        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.reportException(t); // Report any errors properly to active Span.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }

    public MessageDTO secondThingInNewSpan(MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name

        Span bopSpan = tracingHelper.nextSpan(opName);
        try (SpanScoped spanScoped = tracingHelper.startSpan(bopSpan, opName)) {
            log.info("BusinessOperation.{} START", opName);

            tracingHelper.tag(bopSpan, "myFancyTag", "myFancyTagValue");
            tracingHelper.annotate(bopSpan, "here something fancy happened ...");
            log.info("tagged and annotated current span fancy stuff");
            simulateBusinessLogic(payload.seq, Halfmax.HALFMAX);
            // zipkin.annotateCurrentSpan("secondThingAfterSimulation");
            bopSpan.annotate("middleOfSecondThing");
            simulateBusinessLogic(payload.seq, Halfmax.HALFMAX);
            MessageDTO transformedPayload = payload.transform("secondThing Transformation", opName);

            return transformedPayload;

        } catch (Throwable t) {
            // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
            // we have to get back its Span into Scope (=active)
            // chunk will be reported as it was before this try
            tracingHelper.tracer().withSpanInScope(bopSpan);
            log.error("Exception on message transform: {}", t);
            tracingHelper.reportException(t);; // Report any errors properly.
            throw t;
        } finally {
            log.info("BusinessOperation.{} END", opName);
            tracingHelper.finishSpan(bopSpan); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
        }
    }
    
    public MessageDTO thirdThingWithNewDynamicBaggage(MessageDTO payload, String nextBp) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name

        try (ChunkScoped chunkScoped = tracingHelper.startChunk(opName)) {
            log.info("BusinessOperation.{} START", opName);

            // so maybe at this point we know more specific which downstream calls are possible
            // customBaggage.dynBaggageTagSuccessorProcess(nextBp);
            // if you want this change to be propagated to zipkin, you have to start a new tagged
            // span
            // as we will do now ...
            log.info("BusinessOperation.{} new dynamic Baggage", opName);
            
            simulateBusinessLogic(payload.seq);
            MessageDTO transformedPayload = payload.transform("thirdThing Transformation", opName);
            
            return transformedPayload;

        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.reportException(t);; // Report any errors properly.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }
    
    public MessageDTO fourthThingWithScopedBOP(MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name

        try (ChunkScoped chunkScoped = tracingHelper.startChunk(opName)) {
            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic(payload.seq);
            MessageDTO transformedPayload = payload.transform("fourthThing Transformation", opName);
            
            return transformedPayload;

        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.reportException(t);; // Report any errors properly.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }

    // ==============================================================================
    // ==============================================================================
    // ==============================================================================

    public enum Halfmax {
        HALFMAX
    }

    protected Integer callCount = 0;
    protected Integer failOnEveryXthCall = 0;

    public void failOnEveryXthCall(Integer x) {
        log.info("will fail on every {} call now", x);
        this.failOnEveryXthCall = x;
    }

    private void simulateBusinessLogic(String seq, Halfmax... halfmax) throws BusinessException {
        callCount++;
        if ((failOnEveryXthCall > 0) && ((callCount % failOnEveryXthCall) == 0)) {
            log.error("failed on purpose on call {} with BusinessException", callCount);
            throw new BusinessException(
                    String.format("{}:{}:{} failed on {}th call with failOnEveryXthCall {}",
                            BusinessLogic.class.getSimpleName(), tier, instanceIndex, callCount, failOnEveryXthCall));
        }

        Random rand = new Random(System.currentTimeMillis());
        long sleeptime = sleepMin + rand.nextInt(sleepMax - sleepMin);
        if ((halfmax != null) && (halfmax.length > 0)) {
            sleeptime = sleepMin + rand.nextInt((sleepMax / 2) - sleepMin);
        }
        try {
            log.info("[{} in {}] send: {} sleep ms:{}", seq, instanceIndex, tier, sleeptime);
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            log.error("[{} in {}] send: {} Thread.sleep: {}", seq, instanceIndex, tier, e.getMessage());
        }
    }

}
