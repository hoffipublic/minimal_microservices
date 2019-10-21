package com.hoffi.minimal.microservices.microservice.contractbaseclasses;

import com.hoffi.minimal.microservices.microservice.contractbaseclasses.SourceBaseClass;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.StringReader;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;
import static org.springframework.cloud.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;
import static org.springframework.cloud.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;
import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.*;
import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.fileToBytes;

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
			assertThat(response.getHeader("BOOK-NAME")).isNotNull();
			assertThat(response.getHeader("BOOK-NAME").toString()).isEqualTo("foo");
			assertThat(response.getHeader("contentType")).isNotNull();
			assertThat(response.getHeader("contentType").toString()).isEqualTo("application/json");
		// and:
			DocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));
			assertThatJson(parsedJson).field("['seq']").isEqualTo("42");
			assertThatJson(parsedJson).field("['message']").isEqualTo("per contract fromSource");
			assertThatJson(parsedJson).field("['modifications']").isEqualTo("");
	}

}
