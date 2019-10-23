package com.hoffi.minimal.microservices.microservice.contractbaseclasses;

import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import javax.inject.Inject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;

public class OutboundTestCopy extends SourceBaseClass {

	@Inject ContractVerifierMessaging contractVerifierMessaging;
	@Inject ContractVerifierObjectMapper contractVerifierObjectMapper;

	@Test
	public void validate_contractSourceName() throws Exception {
		// when:
			methodFromContract();

		// then:
			ContractVerifierMessage response = contractVerifierMessaging.receive("minimal-SourceTo1");
			assertThat(response).isNotNull();
			assertThat(response.getHeader("baggage_ddd")).isNotNull();
			assertThat(response.getHeader("baggage_ddd").toString()).isEqualTo("testBPDomain");
			assertThat(response.getHeader("baggage_bp")).isNotNull();
			assertThat(response.getHeader("baggage_bp").toString()).isEqualTo("testBProcess");
			assertThat(response.getHeader("baggage_bpids")).isNotNull();
			assertThat(response.getHeader("baggage_bpids").toString()).isEqualTo("43,44");
			assertThat(response.getHeader("contentType")).isNotNull();
			assertThat(response.getHeader("contentType").toString()).isEqualTo("application/json");
		// and:
			DocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));
			assertThatJson(parsedJson).field("['seq']").isEqualTo("42");
			assertThatJson(parsedJson).field("['message']").isEqualTo("per contract fromSource");
			assertThatJson(parsedJson).field("['modifications']").isEqualTo("");
	}

}
