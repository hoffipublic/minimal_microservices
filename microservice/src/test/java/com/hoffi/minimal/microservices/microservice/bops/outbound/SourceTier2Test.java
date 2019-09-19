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
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import annotations.AppTest;
import annotations.MessagingTest;
import annotations.TrivialTest;
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

    @TrivialTest
    @Disabled("Not yet implemented")
    void failTests() {
        fail("Not yet implemented");
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
        BOP modulebop = BOP.initModule("testms", "1", "testmodule");
        BOP bop = modulebop.createBOP("fromSource");
        MessageDTO messageDTO = MessageDTO.create(bop);
        messageDTO.seq = 42;
        messageDTO.message = "fromSource";

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
