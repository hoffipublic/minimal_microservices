package com.hoffi.minimal.microservices.microservice.contractbaseclasses;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels;
import com.hoffi.minimal.microservices.microservice.bops.outbound.Source;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier1;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier2;
import com.hoffi.minimal.microservices.microservice.common.dto.DTOTestHelper;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"source", "unscheduled"})
@SpringBootTest(properties = {"--spring.autoconfigure.exclude="})
@AutoConfigureCache
@AutoConfigureMessageVerifier
// @DirtiesContext // instead of properties = { "--spring.autoconfigure.exclude="}
public abstract class SourceBaseClass {
	@Autowired
	@Qualifier(SourceChannels.OUTPUT)
	MessageChannel sourceOutputMessageChannel;

	@Autowired
	SourceChannels sourceChannels;

	@Inject
	MessageVerifier messaging;

	@Autowired(required = false)
	Source source;
	@Autowired(required = false)
	SourceTier1 sourceTier1;
	@Autowired(required = false)
	SourceTier2 sourceTier2;

	@Before
	public void setup() {
		// com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels.OUTPUT
		this.messaging.receive("minimal-SourceTo1", 100, TimeUnit.MILLISECONDS);
	}

	protected void methodFromContract() {
		MessageDTO referenceMessageDTO = DTOTestHelper.getPlainMessageDTO(); // as default
																				// constructor is
																				// private/protected
		referenceMessageDTO.seq = "42";
		referenceMessageDTO.message = "per contract fromSource";
		referenceMessageDTO.modifications = "";

		sourceChannels.sourceOutput()
				.send(MessageBuilder.withPayload(referenceMessageDTO)
						.setHeaderIfAbsent("baggage_ddd", "testBPDomain")
						.setHeaderIfAbsent("baggage_bp", "testBProcess")
						.setHeaderIfAbsent("baggage_bpids", "43,44").build());
	}
}
