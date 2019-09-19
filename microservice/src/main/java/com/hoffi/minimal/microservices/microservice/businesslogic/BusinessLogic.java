package com.hoffi.minimal.microservices.microservice.businesslogic;

import java.util.Random;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.MDCLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    // @Autowired
    // private CustomBaggage customBaggage;
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

    public MessageDTO firstThing(BOP bop, MessageDTO payload) throws BusinessException {
        String newChunk = new Object() {
        }.getClass().getEnclosingMethod().getName();
        String oldChunk = MDCLocal.startChunk(newChunk);
        try {

            log.info("BusinessOperation.{} START", newChunk);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "firstThing Transformation",
                    String.format(" --> %s>%s:%s-%s-%s", newChunk, bop.bop, bop.microservice, bop.instanceIndex, bop.businessModule));

            log.info("BusinessOperation.{} END", newChunk);

            return transformedPayload;
        } finally {
            MDCLocal.endChunk(newChunk, oldChunk);
        }
    }

    /** has to be in another bean as its caller for a new span to be started by spring */
    // @NewSpan("secondThing")
    public MessageDTO secondThingInANewSpan(BOP bop, MessageDTO payload) throws Exception {
        String newChunk = new Object() {
        }.getClass().getEnclosingMethod().getName();
        String oldChunk = MDCLocal.startChunk(newChunk);
        try {
            log.info("BusinessOperation.{} START", newChunk);

            // zipkin.tagCurrentSpan(Zipkin.TAGKEY.MYFANCY, "myFancyTagValue");
            log.info("tagged current span with myFancyTag");
            simulateBusinessLogic(Halfmax.HALFMAX);
            // zipkin.annotateCurrentSpan("secondThingAfterSimulation");
            simulateBusinessLogic(Halfmax.HALFMAX);
            MessageDTO transformedPayload = payload.transform(bop, "secondThing Transformation",
                    String.format(" --> %s>%s:%s-%s-%s", newChunk, bop.bop, bop.microservice, bop.instanceIndex, bop.businessModule));

            log.info("BusinessOperation.{} END", newChunk);

            return transformedPayload;

        } finally {
            MDCLocal.endChunk(newChunk, oldChunk);
        }
    }

    /** has to be in another bean as its caller for a new span to be started by spring */
    // @NewSpan("thirdThing")
    public MessageDTO thirdThingInANewSpanAndNewDynamicBaggage(BOP bop, MessageDTO payload,
            String nextBp) throws Exception {
        String newChunk = new Object() {
        }.getClass().getEnclosingMethod().getName();
        String oldChunk = MDCLocal.startChunk(newChunk);
        try {
            log.info("BusinessOperation.{} START", newChunk);

            // so maybe at this point we know more specific which downstream calls are possible
            // customBaggage.dynBaggageTagSuccessorProcess(nextBp);
            // if you want this change to be propagated to zipkin, you have to start a new tagged
            // span
            // as we will do now ...
            log.info("BusinessOperation.{} new dynamic Baggage", newChunk);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "thirdThing Transformation",
                    String.format(" --> %s>%s:%s-%s-%s", newChunk, bop.bop, bop.microservice, bop.instanceIndex, bop.businessModule));

            log.info("BusinessOperation.{} END", newChunk);

            return transformedPayload;
        } finally {
            MDCLocal.endChunk(newChunk, oldChunk);
        }
    }

    public MessageDTO fourthBusinessLogicWithoutNewSpan(BOP bop, MessageDTO payload)
            throws Exception {
        String newChunk = new Object() {
        }.getClass().getEnclosingMethod().getName();
        String oldChunk = MDCLocal.startChunk(newChunk);
        try {
            log.info("BusinessOperation.{} START", newChunk);

            simulateBusinessLogic();
            MessageDTO transformedPayload = payload.transform(bop, "fourthThing Transformation",
                    String.format(" --> %s>%s:%s-%s-%s", newChunk, bop.bop, bop.microservice, bop.instanceIndex, bop.businessModule));

            log.info("BusinessOperation.{} END", newChunk);

            return transformedPayload;
        } finally {
            MDCLocal.endChunk(newChunk, oldChunk);
        }
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
