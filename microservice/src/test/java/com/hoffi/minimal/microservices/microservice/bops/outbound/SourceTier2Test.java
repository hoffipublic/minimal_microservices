package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier2Channels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.ScopedChunk;
import com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging.TracingHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import annotations.AppTest;
import annotations.MessagingTest;
import annotations.TrivialTest;
import io.opentracing.Span;
import testhelpers.DTOhelpers;

@ActiveProfiles("tier2")
@TestPropertySource(properties = { "spring.application.name=microservice_tier2", "app.businessLogic.tier=tier2",
        "eureka.client.enabled=false", "spring.cloud.config.enabled=false", "management.endpoints.enabled-by-default=false",
        "management.endpoints.web.exposure.exclude=\"*\"" })
//@ImportAutoConfiguration(classes = { RefreshAutoConfiguration.class, TestSupportBinderAutoConfiguration.class,
//        MessageCollectorAutoConfiguration.class })
//@ContextConfiguration(classes = { MessageCollector.class, StreamBindingsConfig.class, CustomBaggage.class, Zipkin.class, SleuthConfig.class, Sourcetier2.class })
//@JsonTest // don't know but test does not start at all with this
// @EnableCircuitBreaker
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest
class SourceTier2Test extends DTOhelpers {
    // import static org.assertj.core.api.Assertions.assertThat;
    // import static org.junit.jupiter.api.Assertions.assertEquals;
    // import static org.junit.jupiter.api.Assertions.fail;

    @Value("${app.businessLogic.tier}")
    private String tier;
    @Value("${app.info.instance_index}")
    private String instanceIndex;

    @Autowired
    private JacksonTester<MessageDTO> json;

    // @Autowired
    // private CustomBaggage customBaggage;

    @Autowired
    @Qualifier(Tier2Channels.OUTPUT)
    private MessageChannel tier2OutputMessageChannel;

    //    @Autowired
    //    @Qualifier(Tier2Channels.INPUT)
    //    private SubscribableChannel tier2InputSubscribableChannel;

    @Autowired
    Tier2Channels tier2Channels;

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    TracingHelper tracingHelper;

    @TrivialTest
    @Disabled("Not yet implemented")
    void failTests() {
        fail("Not yet implemented");
    }

    @AppTest
    @MessagingTest
    void minimalSendTest() {
        // construct test MessageDTO to be send
        String testOpName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        testOpName = SourceTier1.class.getSimpleName() + "." + testOpName;

        BOP opBOP = BOP.initInitially("testDomain", "testProcess", testOpName, "42", "5");
        // Start a new Trace for BOP
        Span bopSpan = tracingHelper.tracer().buildSpan(testOpName).start();
        try (ScopedChunk scopedChunkBusinessLogic = tracingHelper.startScopedChunk(bopSpan, opBOP, "timerSend", true)) {
            BOP scopeBOP = scopedChunkBusinessLogic.bop();
            MessageDTO messageDTO = MessageDTO.create(scopeBOP, "testMessage", "initial Modification");
            messageDTO.seq = 42;

            Message<MessageDTO> messageToSend = MessageBuilder.withPayload(messageDTO)
            // .setHeader("X-B3-TraceId", "testTrace").setHeader("X-B3-SpanId", "testSpan")
            .build();

            tier2Channels.tier2Input().send(messageToSend);
        } catch (Throwable t) {
            bopSpan.log(t.getMessage()); // Report any errors properly.
        }
        
        @SuppressWarnings("unchecked")
        Message<MessageDTO> receivedMessage =
                (Message<MessageDTO>) messageCollector.forChannel(tier2Channels.tier2Output()).poll();

        // // extract receivedDTO from message and do assertions
        // String payload = (String) receivedMessage.getPayload();
        // MessageDTO receivedDTO = json.parse(payload).getObject();
        MessageDTO receivedDTO = receivedMessage.getPayload();

        assertEquals("testMessage", receivedDTO.message);
    }
    
    @AppTest
    @MessagingTest
    void functionalSendTest() throws IOException, InterruptedException {
        AbstractMessageChannel tier2Input = (AbstractMessageChannel) this.tier2Channels.tier2Input();
        AbstractMessageChannel tier2Output = (AbstractMessageChannel) this.tier2Channels.tier2Output();

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
        tier2Output.addInterceptor(assertionInterceptor);

        // construct test MessageDTO to be send
        String testOpName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        testOpName = SourceTier1.class.getSimpleName() + "." + testOpName;

        BOP opBOP = BOP.initInitially("testDomain", "testProcess", testOpName, "42", "5");
        MessageDTO messageDTO = MessageDTO.create(opBOP, "testMessage", "initial Modification");
        messageDTO.seq = 42;

        // start Trace and  and send messageDTO
        //customBaggage.startTrace("testBP", 42, "nextMS");
        Message<MessageDTO> messageToSend = MessageBuilder.withPayload(messageDTO)
                // .setHeader("X-B3-TraceId", "testTrace").setHeader("X-B3-SpanId", "testSpan")
                .build();
        tier2Input.send(messageToSend);

        // receive via assertionInterceptor's set messageAtomicReference
        messageWasCompletelyProcesses.await(500, TimeUnit.MILLISECONDS);
        Message<?> receivedMessage = atomicReference.get();
        assertThat(receivedMessage).isNotNull();

        // extract receivedDTO from message and do assertions
        String payload = (String) receivedMessage.getPayload();
        MessageDTO receivedDTO = json.parse(payload).getObject();
        // @formatter:off
        Assertions.assertAll(
            () -> assertEquals(Integer.valueOf(42), receivedDTO.seq),
            () -> assertEquals("fourthBusinessLogicWithoutNewSpanTransformation", receivedDTO.message),
            () -> assertEquals(" ==> tier2:i0:firstThing ==> tier2:i0:secondThingInANewSpan ==> tier2:i0:thirdThingInANewSpanAndNewDynamicBaggage ==> tier2:i0:fourthBusinessLogicWithoutNewSpan", receivedDTO.modifications)
        );
        // @formatter:on
    }
}
