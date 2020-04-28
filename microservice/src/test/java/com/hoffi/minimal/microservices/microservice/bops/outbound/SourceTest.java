package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import annotations.AppTest;
import annotations.MessagingTest;
import testhelpers.DTOhelpers;

@ActiveProfiles({"test", "source"})
// @TestPropertySource(properties = { "app.sources.fixedDelay=500", "spring.application.name=microservice_source",
//         "app.businessLogic.tier=source", "eureka.client.enabled=false", "spring.cloud.config.enabled=false",
//         "management.endpoints.enabled-by-default=false", "management.endpoints.web.exposure.exclude='*''",
//         "management.health.binders.enabled=false" })
//@ImportAutoConfiguration(classes = { RefreshAutoConfiguration.class, MessageCollectorAutoConfiguration.class })
//@ContextConfiguration(classes = { RefreshableScheduler.class, SchedulingRate.class, StreamBindingsConfig.class, MessageCollector.class,
//        CustomBaggage.class, Zipkin.class, SleuthConfig.class, Source.class })
@EnableScheduling
//@JsonTest // don't know but test does not start at all with this
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest
//@EnableAutoConfiguration(exclude = {})
@Import(TestChannelBinderConfiguration.class)
class SourceTest extends DTOhelpers {
    private static final Logger log = LoggerFactory.getLogger(SourceTest.class);

    // import static org.hamcrest.Matchers.is;
    // import static org.junit.Assert.assertThat;
    // import static org.junit.jupiter.api.Assertions.assertEquals;
    // import static org.junit.jupiter.api.Assertions.fail;
    // import org.springframework.integration.support.MessageBuilder;
    // import org.springframework.integration.channel.AbstractMessageChannel;

    @Autowired
    private JacksonTester<MessageDTO> json;

    @Autowired
    @Qualifier(SourceChannels.OUTPUT)
    private MessageChannel sourceOutputMessageChannel;

    @Autowired
    SourceChannels sourceChannels;

    @Autowired
    ConfigurableApplicationContext context;

    /**
     * test if source generates messages via RefreshableScheduler in SchedulingRate
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @AppTest
    @MessagingTest
    void timerMessageSourceTest() throws IOException, InterruptedException {

        MessageDTO referenceMessageDTO = TH.referenceMessageDTO();
        referenceMessageDTO.seq = "1";
        referenceMessageDTO.message = TH.REF_MESSAGE_TIMER;

        // every receive for test below increases MessageDTO seq

//        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "test,source");
//        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
//                TestChannelBinderConfiguration.getCompleteConfiguration(
////                         StreamBindingsConfig.class, Source.class, TracingHelper.class, SleuthConfig.class, ZipkinConfig.class, ZipkinAutoConfiguration.class, TraceBaggageAutoConfiguration.class, TraceAutoConfiguration.class
////                )).web(WebApplicationType.NONE).run("--spring.zipkin.enabled=false", "--spring.zipkin.discoveryClientEnabled=false"))
//                        MicroserviceApplication.class)).run("--spring.zipkin.enabled=false", "--spring.zipkin.discoveryClientEnabled=false"))
//        {
//            context.getEnvironment().setActiveProfiles("test", "source");
            log.info("SPRING_ACTIVE_PROFILES=" + Arrays.stream(context.getEnvironment().getActiveProfiles()).collect(Collectors.joining(",")));

            OutputDestination target = context.getBean(OutputDestination.class);
            Message<byte[]> receivedMessageBytes = target.receive(10000, SourceChannels.OUTPUT);
            byte[] receivedBytes = receivedMessageBytes.getPayload();
            MessageDTO receivedDTO = json.parse(receivedBytes).getObject();
            log.info("receivedDTO = {}", receivedDTO);
            assertEquals(referenceMessageDTO.toString(), receivedDTO.toString());

            // @formatter:off
            Assertions.assertAll(
//                    () -> assertEquals("3", receivedDTO.seq, "seq"),
                    () -> assertTrue(Integer.parseInt(receivedDTO.seq) <= 5, "seq"),
                    () -> assertEquals(referenceMessageDTO.message, receivedDTO.message, "message"),
                    () -> assertEquals(referenceMessageDTO.modifications, receivedDTO.modifications, "modifications")
            );
            // @formatter:on
//        }
    }
}
