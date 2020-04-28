package com.hoffi.minimal.microservices.microservice.contractbaseclasses;

import com.hoffi.minimal.microservices.microservice.contractbaseclasses.SourceBaseClass;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import javax.inject.Inject;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

import static org.springframework.cloud.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;
import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.*;
import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;
import static org.springframework.cloud.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;
import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.fileToBytes;

@SuppressWarnings("rawtypes")
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

		// and:
			assertThat(response.getHeader("ddd")).isNotNull();
			assertThat(response.getHeader("ddd").toString()).isEqualTo("testBPDomain");
			assertThat(response.getHeader("bp")).isNotNull();
			assertThat(response.getHeader("bp").toString()).isEqualTo("testBProcess");
			assertThat(response.getHeader("bpids")).isNotNull();
			assertThat(response.getHeader("bpids").toString()).isEqualTo("43,44");
			assertThat(response.getHeader("contentType")).isNotNull();
			assertThat(response.getHeader("contentType").toString()).isEqualTo("application/json");

		// and:
			DocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));
			assertThatJson(parsedJson).field("['seq']").isEqualTo("42");
			assertThatJson(parsedJson).field("['message']").isEqualTo("per contract fromSource");
			assertThatJson(parsedJson).field("['modifications']").isEqualTo("");
	}

}
