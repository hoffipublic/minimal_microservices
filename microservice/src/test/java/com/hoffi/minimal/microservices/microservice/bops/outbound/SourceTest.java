package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.stream.test.matcher.MessageQueueMatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import annotations.AppTest;
import annotations.MessagingTest;
import testhelpers.DTOhelpers;

@ActiveProfiles("source")
@TestPropertySource(properties = { "app.sources.fixedDelay=500", "spring.application.name=microservice_source",
        "app.businessLogic.tier=source", "eureka.client.enabled=false", "spring.cloud.config.enabled=false",
        "management.endpoints.enabled-by-default=false", "management.endpoints.web.exposure.exclude=\"*\"" })
//@ImportAutoConfiguration(classes = { RefreshAutoConfiguration.class, MessageCollectorAutoConfiguration.class })
//@ContextConfiguration(classes = { RefreshableScheduler.class, SchedulingRate.class, StreamBindingsConfig.class, MessageCollector.class,
//        CustomBaggage.class, Zipkin.class, SleuthConfig.class, Source.class })
@EnableScheduling
// @EnableCircuitBreaker
//@JsonTest // don't know but test does not start at all with this
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest
class SourceTest extends DTOhelpers {

    // import static org.hamcrest.Matchers.is;
    // import static org.junit.Assert.assertThat;
    // import static org.junit.jupiter.api.Assertions.assertEquals;
    // import static org.junit.jupiter.api.Assertions.fail;
    // import org.springframework.integration.support.MessageBuilder;
    // import org.springframework.integration.channel.AbstractMessageChannel;


    @Autowired
    private MessageCollector collector;

    @Autowired
    private JacksonTester<MessageDTO> json;

    @Autowired
    @Qualifier(SourceChannels.OUTPUT)
    private MessageChannel sourceOutputMessageChannel;

    @Autowired
    SourceChannels sourceChannels;

    /**
     * test if source generates messages via RefreshableScheduler in SchedulingRate
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @AppTest
    @MessagingTest
    void timerMessageSourceTest() throws IOException, InterruptedException {

        BlockingQueue<Message<?>> messages = collector.forChannel(sourceChannels.sourceOutput());

        MessageDTO referenceMessageDTO = TH.referenceMessageDTO();
        referenceMessageDTO.bop.setBopIds("1");
        referenceMessageDTO.bop.chunk = TH.REF_BOB_CHUNK_TIMER;
        referenceMessageDTO.seq = 1;
        referenceMessageDTO.message = TH.REF_MESSAGE_TIMER;

        // every receive for test below increases MessageDTO seq and BOP.bpId

        // Option 1: receive message tests with MessageQueueMatcher
        JsonContent<MessageDTO> referenceJsonDTO = this.json.write(referenceMessageDTO);
        assertThat(messages, MessageQueueMatcher.receivesPayloadThat(is(referenceJsonDTO.getJson())));

        //        // 2. receive message tests with MessageQueueMatcher
        //        // but needing FeatureMatcher implementations
//        // @formatter:off
//        assertEquals(messages, MessageQueueMatcher.receivesPayloadThat(allOf(
//            super.id(equalTo(Integer.valueOf(2))),
//            super.message(equalTo("fromSource")),
//            super.modifiers(equalTo(""))
//        )));
//        // @formatter:on

        // 3. receive message by calling poll on BlockingQueue by ourself
        // but having to convert the payload also by ourself
        Message<?> receivedMessage = messages.poll(5, TimeUnit.SECONDS);
        String payload = (String) receivedMessage.getPayload();
        MessageDTO receivedDTO = json.parse(payload).getObject();

        // @formatter:off
        Assertions.assertAll(
            () -> assertEquals(Integer.valueOf(2), receivedDTO.seq, "seq"),
            () -> assertEquals(referenceMessageDTO.message, receivedDTO.message, "message"),
            () -> assertEquals(referenceMessageDTO.modifications, receivedDTO.modifications, "modifications"),
            () -> assertEquals("2", receivedDTO.bop.toStringBopIds(), "bpIDs"), // as it was the second call to timerMessageSource 
            () -> assertEquals(referenceMessageDTO.bop.businessDomain, receivedDTO.bop.businessDomain, "businessDomain"),
            () -> assertEquals(referenceMessageDTO.bop.businessProcess, receivedDTO.bop.businessProcess, "businessProcess"),
            () -> assertEquals(referenceMessageDTO.bop.instanceIndex, receivedDTO.bop.instanceIndex, "instanceIndex"),
            () -> assertEquals(referenceMessageDTO.bop.operation, receivedDTO.bop.operation, "operation"),
            () -> assertEquals(referenceMessageDTO.bop.chunk, receivedDTO.bop.chunk, "chunk")
        );
        // @formatter:on
    }
}
