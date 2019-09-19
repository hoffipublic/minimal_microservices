package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
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

        // first received message simple check
        BOP modulebop = BOP.initModule("testms", "1", "testmodule");
        BOP bop = modulebop.createBOP("fromSource");
        MessageDTO messageDTO = MessageDTO.create(bop);
        messageDTO.seq = 1;
        messageDTO.message = "fromSource";
        JsonContent<MessageDTO> dtoJson = this.json.write(messageDTO);
        assertEquals(messages, MessageQueueMatcher.receivesPayloadThat(is(dtoJson.getJson())));

        //        // second received message tests with MessageQueueMatcher
        //        // but needing FeatureMatcher implementations
//        // @formatter:off
//        assertEquals(messages, MessageQueueMatcher.receivesPayloadThat(allOf(
//            super.id(equalTo(Integer.valueOf(2))),
//            super.message(equalTo("fromSource")),
//            super.modifiers(equalTo(""))
//        )));
//        // @formatter:on

        // third received message by calling poll on BlockingQueue by ourself
        // but having to convert the payload also by ourself
        Message<?> receivedMessage = messages.poll(5, TimeUnit.SECONDS);
        String payload = (String) receivedMessage.getPayload();
        MessageDTO receivedDTO = json.parse(payload).getObject();

        // @formatter:off
        Assertions.assertAll(
            () -> assertEquals(Integer.valueOf(2), receivedDTO.seq),
            () -> assertEquals("fromSource", receivedDTO.message),
            () -> assertEquals("", receivedDTO.modifications)
        );
        // @formatter:on
    }

}
