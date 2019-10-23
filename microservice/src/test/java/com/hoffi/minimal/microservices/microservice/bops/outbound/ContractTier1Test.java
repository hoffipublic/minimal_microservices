package com.hoffi.minimal.microservices.microservice.bops.outbound;

import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import javax.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hoffi.minimal.microservices.microservice.bops.channels.Tier1Channels;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"tier1"})
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SpringBootTest
// (
// 	properties = {
// 		"--spring.autoconfigure.exclude=" })
@AutoConfigureStubRunner(ids = {"demo:minimal_microservice:+:stubs"},
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
        )
@AutoConfigureCache
public class ContractTier1Test {
    @Autowired
    StubTrigger stubTrigger;

    @Inject
    ContractVerifierMessaging contractVerifierMessaging;
    @Inject
    ContractVerifierObjectMapper contractVerifierObjectMapper;

    @Autowired
    Tier1Channels tier1Channels;

    @Test
    public void shouldConformToContract() throws JsonProcessingException {
        // label of contract
        stubTrigger.trigger("contractSourceLabel"); // triggers contracted message receive

        // here c.h.m.m.m.bops.inbound.SinkTier1 and outbound.SourceTier1 do their business operations

        // and we check against the send out result after business operations finished
        ContractVerifierMessage response = contractVerifierMessaging.receive("minimal-1To2");
        assertNotNull(response);

        assertNotNull(response.getHeader("baggage_ddd"));
        assertEquals("testBPDomain", response.getHeader("baggage_ddd").toString());
        assertNotNull(response.getHeader("baggage_bp"));
        assertEquals("testBProcess", response.getHeader("baggage_bp").toString());
        assertNotNull(response.getHeader("baggage_bpids"));
        assertEquals("43,44", response.getHeader("baggage_bpids").toString());
        assertNotNull(response.getHeader("contentType"));
        assertEquals("application/json", response.getHeader("contentType").toString());
        // and:
        DocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));
        assertThatJson(parsedJson).field("['seq']").isEqualTo("42");
        assertThatJson(parsedJson).field("['message']").isEqualTo("transformed by manualSpan");
        assertThatJson(parsedJson).field("['modifications']").isEqualTo("firstThingWithinSameSpan --> secondThingInNewSpan --> thirdThingWithNewDynamicBaggage --> fourthThingWithScopedBOP --> manualSpan");
            
    }
}
