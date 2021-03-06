package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.tracing.SpanScoped;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.ActiveProfiles;
import annotations.AppTest;
import annotations.MessagingTest;
import annotations.TrivialTest;
import brave.Span;
import testhelpers.DTOhelpers;

@ActiveProfiles({"test", "tier1"})
// @TestPropertySource(properties = { "spring.application.name=microservice_tier1",
//         "app.businessLogic.tier=tier1", "eureka.client.enabled=false", "spring.cloud.config.enabled=false",
//         "management.endpoints.enabled-by-default=false", "management.endpoints.web.exposure.exclude='*''",
//         "management.health.binders.enabled=false" })
//@ImportAutoConfiguration(classes = { RefreshAutoConfiguration.class, TestSupportBinderAutoConfiguration.class,
//        MessageCollectorAutoConfiguration.class })
//@ContextConfiguration(classes = { MessageCollector.class, StreamBindingsConfig.class, CustomBaggage.class, Zipkin.class, SleuthConfig.class, SourceTier1.class })
//@JsonTest // don't know but test does not start at all with this
// @EnableCircuitBreaker
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest
class SourceTier1Test extends DTOhelpers {
    private static Logger log = LoggerFactory.getLogger(SourceTier1Test.class);


 //   import static org.assertj.core.api.Assertions.assertThat;
 //   import static org.junit.jupiter.api.Assertions.assertEquals;
 //   import static org.junit.jupiter.api.Assertions.fail;
//    import org.springframework.integration.support.MessageBuilder;
 //   import org.springframework.integration.channel.AbstractMessageChannel;


    @Value("${app.businessLogic.tier}")
    private String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private TracingHelper tracingHelper;

    @Autowired
    private JacksonTester<MessageDTO> json;

    // @Autowired
    // private CustomBaggage customBaggage;

    @Autowired
    @Qualifier(Tier1Channels.OUTPUT)
    private MessageChannel tier1OutputMessageChannel;

    //    @Autowired
    //    @Qualifier(Tier1Channels.INPUT)
    //    private SubscribableChannel tier1InputSubscribableChannel;

    @Autowired
    Tier1Channels tier1Channels;

    @TrivialTest
    @Disabled("Not yet implemented")
    void failTests() {
        fail("Not yet implemented");
    }

    @AppTest
    @MessagingTest
    void functionalSendTest() throws IOException, InterruptedException {
        AbstractMessageChannel tier1Input = (AbstractMessageChannel) this.tier1Channels.tier1Input();
        AbstractMessageChannel tier1Output = (AbstractMessageChannel) this.tier1Channels.tier1Output();

        final AtomicReference<Message<?>> atomicReference = new AtomicReference<>();
        CountDownLatch messageWasCompletelyProcesses = new CountDownLatch(1);

        ChannelInterceptor assertionInterceptor = new ChannelInterceptor() {
            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
                System.err.println("afterSendCompletion to '" + channel + "':" + message.getPayload());
                atomicReference.set(message);
                messageWasCompletelyProcesses.countDown();
            }

            @Override
            public void afterReceiveCompletion(@Nullable Message<?> message, MessageChannel channel, @Nullable Exception ex) {
                System.out.println(
                        "afterReceiveCompletion to '" + channel + "':" + message.getPayload() + (ex == null ? "\n" : ex.getStackTrace()));
                // messageAtomicReference.set(message);
            }
        };
        tier1Output.addInterceptor(assertionInterceptor);

        // prepare the test by simulate a message send
        String testOpName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        testOpName = SourceTier1.class.getSimpleName() + "." + testOpName;

        // ... start a completely new Trace
        Span opSpan = tracingHelper.newTrace(testOpName);
        try (SpanScoped spanScoped = tracingHelper.startTrace(opSpan, TH.REF_BOB_DOMAIN, TH.REF_BOB_PROCESS, TH.REF_BOBID, testOpName, instanceIndex)) {

            // construct test MessageDTO for test simulation
            MessageDTO messageDTO = MessageDTO.create("testMessage", "initial Modification");
            messageDTO.seq = "42";
            Message<MessageDTO> messageToSend = MessageBuilder.withPayload(messageDTO)
                    // .setHeader("X-B3-TraceId", "testTrace").setHeader("X-B3-SpanId", "testSpan")
                    .build();

            // simulate send
            tier1Input.send(messageToSend);

        } catch (Exception e) {
            // as SpanWithBOP's ScopedBOP and SpanInScope was already autofinished
            // we have to get back its Span into Scope (=active)
            // chunk will be reported as it was before this try
            tracingHelper.tracer().withSpanInScope(opSpan);
            log.error(testOpName, e);
            opSpan.annotate(e.getMessage()); // Report any errors properly.
        } finally {
            tracingHelper.finishSpanAndOperation(opSpan, testOpName); // trace will not be propagated to Zipkin/Jaeger unless explicitly finished
        }

        // receive via assertionInterceptor's set messageAtomicReference
        messageWasCompletelyProcesses.await(500, TimeUnit.MILLISECONDS);
        Message<?> receivedMessage = null;
        receivedMessage = atomicReference.get();
        assertThat(receivedMessage).isNotNull();

        // extract receivedDTO from message and do assertions
        byte[] receivedBytes = (byte[])receivedMessage.getPayload();
        MessageDTO receivedDTO = json.parse(receivedBytes).getObject();


        // @formatter:off
        Assertions.assertAll(
            () -> assertEquals("42", receivedDTO.seq),
            () -> assertEquals("transformed by manualSpan", receivedDTO.message),
            () -> assertEquals("initial Modification --> firstThingWithinSameSpan --> secondThingInNewSpan --> thirdThingWithNewDynamicBaggage --> fourthThingWithScopedBOP --> manualSpan", receivedDTO.modifications)
        );
        // @formatter:on
    }
}
