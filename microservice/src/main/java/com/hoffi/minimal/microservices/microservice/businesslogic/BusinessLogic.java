package com.hoffi.minimal.microservices.microservice.businesslogic;

import java.util.Random;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.MDCKEY;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.ScopedBOP;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.ScopedChunk;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.opentracing.Span;

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

    public MessageDTO firstThingWithinSameChunk(ScopedChunk scopedChunk, MessageDTO payload) throws BusinessException {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try(ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(scopedChunk, opName, true)) {

            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(scopedChunk.bop(), "firstThing Transformation", scopedChunk.bop().toStringMod());

            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            scopedChunk.scope.span().log(t.getMessage()); // Report any errors properly.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }

    /** has to be in another bean as its caller for a new span to be started by spring */
    // @NewSpan("secondThing")
    public MessageDTO secondThingInANewSpan(ScopedChunk scopedChunk, MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        Span bopSpan = tracingHelper.tracer().buildSpan(opName).start();
        try (ScopedChunk scopedChunkSecondThing = tracingHelper.startScopedChunk(bopSpan, scopedChunk.bop(), opName, true)) {
            log.info("BusinessOperation.{} START", opName);
            BOP innerScopeBOP = scopedChunkSecondThing.bop();

            // zipkin.tagCurrentSpan(Zipkin.TAGKEY.MYFANCY, "myFancyTagValue");
            log.info("tagged current span with myFancyTag");
            simulateBusinessLogic(Halfmax.HALFMAX);
            // zipkin.annotateCurrentSpan("secondThingAfterSimulation");
            tracingHelper.tag("manualTag", "manualValue");
            simulateBusinessLogic(Halfmax.HALFMAX);
            MessageDTO transformedPayload = payload.transform(scopedChunk.bop(), "secondThing Transformation", innerScopeBOP.toStringMod());

            return transformedPayload;
            
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            bopSpan.log(t.getMessage()); // Report any errors properly.
            throw t;
        } finally {
            log.info("BusinessOperation.{} END", opName);
            // scopedChunkSecondThing.close();
        }
    }
    
    /** has to be in another bean as its caller for a new span to be started by spring */
    // @NewSpan("thirdThing")
    public MessageDTO thirdThingWithNewDynamicBaggage(ScopedChunk scopedChunk, MessageDTO payload, String nextBp) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try (ScopedBOP scopedBOP = tracingHelper.startUnscopedChunk(scopedChunk, opName, true)) {
            log.info("BusinessOperation.{} START", opName);

            // so maybe at this point we know more specific which downstream calls are possible
            // customBaggage.dynBaggageTagSuccessorProcess(nextBp);
            // if you want this change to be propagated to zipkin, you have to start a new tagged
            // span
            // as we will do now ...
            log.info("BusinessOperation.{} new dynamic Baggage", opName);
            
            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(scopedChunk.bop(), "thirdThing Transformation", scopedChunk.bop().toStringMod());
            
            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            scopedChunk.scope.span().log(t.getMessage()); // Report any errors properly.
        } finally {
            log.info("BusinessOperation.{} END", opName);
        }
        return null;
    }
    
    public MessageDTO fourthThingWithScopedBOP(ScopedChunk scopedChunk, MessageDTO payload) throws Exception {
        String opName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        try(ScopedBOP scopedBOP = new ScopedBOP(scopedChunk, opName, true)) {
            tracingHelper.setMDC(MDCKEY.CHUNK, opName);
            log.info("BusinessOperation.{} START", opName);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(scopedChunk.bop(), "fourthThing Transformation", scopedChunk.bop().toStringMod());

            
            return transformedPayload;
        } catch (Throwable t) {
            log.error("Exception on message transform: {}", t);
            scopedChunk.scope.span().log(t.getMessage()); // Report any errors properly.
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
