package com.hoffi.minimal.microservices.microservice.bops.outbound;

import java.util.HashMap;
import java.util.Map;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles({ "tier1" })
@TestPropertySource(properties = { "spring.application.name=microservice_tier1", "app.businessLogic.tier=tier1",
        "eureka.client.enabled=false", "spring.cloud.config.enabled=false", "management.endpoints.enabled-by-default=false",
        "management.endpoints.web.exposure.exclude=\"*\"" })
//@JsonTest // don't know but test does not start at all with this
// @EnableCircuitBreaker
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest
class SourceTier1ContractTest {

    @Autowired
    @Qualifier(Tier1Channels.OUTPUT)
    private MessageChannel tier1OutputMessageChannel;

    @Test
    public void contractMessagingTestForTier1() {
        BOP modulebop = BOP.initModule("testms", "1", "testmodule");
        BOP bop = modulebop.createBOP("fromSource");
        MessageDTO messageDTO = MessageDTO.create(bop);
        messageDTO.seq = 42;
        messageDTO.message = "Hoffi";
        messageDTO.modifications = "modifiedByHoffisTest";

        Map<String, Object> headers = new HashMap<>();
        headers.put("id", "TestTraceId");
        headers.put("baggage-bpn", "testbpn");
        headers.put("baggage-bpid", "42");
        headers.put("baggage-succ", "nexttest");
        headers.put("contentType", "application/json");

        //super.messageVerifier.send(messageDTO, headers, Tier1Channels.INPUT);

    }
}
