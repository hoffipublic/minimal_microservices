package com.hoffi.minimal.microservices.microservice.businesslogic;

import java.util.Random;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.tracing.ScopedBOP;
import com.hoffi.minimal.microservices.microservice.tracing.SpanWithBOP;
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

    // @Autowired
    // private Zipkin zipkin;

    // public BusinessLogic() {
    // this.tier = "DEFAULT";
    // this.instanceId = "42";
    // log.error("DEFAULT constructor of BusinessLogic class called for Autowiring ... should not
    // happen!!!");
    // try {
    // throw new Exception();
    // } catch (Exception ex) {
    // ex.printStackTrace();
    // }
    // }

    public BusinessLogic() {
    }

    public MessageDTO firstThingWithinSameSpan(SpanWithBOP spanWithBOP, MessageDTO payload) throws BusinessException {
        return firstThingWithinSameSpan(spanWithBOP.bop(), payload);
    }
    public MessageDTO firstThingWithinSameSpan(BOP bop, MessageDTO payload) throws BusinessException {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try(ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(bop, opName)) {

            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "firstThing Transformation", bop.toStringMod());

            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.annotate(t.getMessage()); // Report any errors properly to active Span.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }

    public MessageDTO secondThingInNewSpan(SpanWithBOP spanWithBOP, MessageDTO payload) throws Exception {
        return secondThingInNewSpan(spanWithBOP.bop(), payload);
    }
    public MessageDTO secondThingInNewSpan(BOP bop, MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        Span bopSpan = tracingHelper.tracer().nextSpan().name(opName);
        try (SpanWithBOP spanWithBOPSecondThing = tracingHelper.startSpan(bopSpan, bop, opName)) {
            log.info("BusinessOperation.{} START", opName);
            BOP innerScopeBOP = spanWithBOPSecondThing.bop();

            tracingHelper.tag(bopSpan, "myFancyTag", "myFancyTagValue");
            tracingHelper.annotate(bopSpan, "here something fancy happened ...");
            log.info("tagged and annotated current span fancy stuff");
            simulateBusinessLogic(Halfmax.HALFMAX);
            // zipkin.annotateCurrentSpan("secondThingAfterSimulation");
            bopSpan.annotate("middleOfSecondThing");
            simulateBusinessLogic(Halfmax.HALFMAX);
            MessageDTO transformedPayload = payload.transform(bop, "secondThing Transformation", innerScopeBOP.toStringMod());

            return transformedPayload;
            
        } catch (Throwable t) {
            // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
            // we have to get back its Span into Scope (=active)
            // chunk will be reported as it was before this try
            tracingHelper.tracer().withSpanInScope(bopSpan);
            log.error("Exception on message transform: {}", t);
            bopSpan.annotate(t.getMessage()); // Report any errors properly.
            throw t;
        } finally {
            log.info("BusinessOperation.{} END", opName);
            tracingHelper.finishSpan(bopSpan, bop); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
        }
    }
    
    public MessageDTO thirdThingWithNewDynamicBaggage(SpanWithBOP spanWithBOP, MessageDTO payload, String nextBp) throws Exception {
        return thirdThingWithNewDynamicBaggage(spanWithBOP.bop(), payload, nextBp);
    }
    public MessageDTO thirdThingWithNewDynamicBaggage(BOP bop, MessageDTO payload, String nextBp) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try (ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(bop, opName)) {
            log.info("BusinessOperation.{} START", opName);

            // so maybe at this point we know more specific which downstream calls are possible
            // customBaggage.dynBaggageTagSuccessorProcess(nextBp);
            // if you want this change to be propagated to zipkin, you have to start a new tagged
            // span
            // as we will do now ...
            log.info("BusinessOperation.{} new dynamic Baggage", opName);
            
            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "thirdThing Transformation", bop.toStringMod());
            
            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.annotate(t.getMessage()); // Report any errors properly.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }
    
    public MessageDTO fourthThingWithScopedBOP(SpanWithBOP spanWithBOP, MessageDTO payload) throws Exception {
        return fourthThingWithScopedBOP(spanWithBOP.bop(), payload);
    }
    public MessageDTO fourthThingWithScopedBOP(BOP bop, MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try(ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(bop, opName)) {
            // tracingHelper.setMDC(MDCKEY.CHUNK, opName);
            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "fourthThing Transformation", bop.toStringMod());

            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            tracingHelper.annotate(t.getMessage()); // Report any errors properly.
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

    private void simulateBusinessLogic(Halfmax... halfmax) throws BusinessException {
        callCount++;
        if ((failOnEveryXthCall > 0) && ((callCount % failOnEveryXthCall) == 0)) {
            log.error("failed on purpose on call {} with BusinessException", callCount);
            throw new BusinessException(
                    String.format("{}:{}:{} failed on {}th call with failOnEveryXthCall {}",
                            BusinessLogic.class.getSimpleName(), tier, instanceIndex, callCount,
                            failOnEveryXthCall));
        } else {
            log.debug("not failing on purpose {} failOnEveryXthCall {}", callCount,
                    failOnEveryXthCall);
        }

        Random rand = new Random(System.currentTimeMillis());
        long sleeptime = sleepMin + rand.nextInt(sleepMax - sleepMin);
        if ((halfmax != null) && (halfmax.length > 0)) {
            sleeptime = sleepMin + rand.nextInt((sleepMax / 2) - sleepMin);
        }
        try {
            log.info("[{}] {} sleep ms:{}", instanceIndex, tier, sleeptime);
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            log.error("[{}] {} Thread.sleep: {}", instanceIndex, tier, e.getMessage());
        }
    }

}
