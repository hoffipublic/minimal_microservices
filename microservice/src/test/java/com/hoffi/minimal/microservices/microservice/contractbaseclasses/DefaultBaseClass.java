package com.hoffi.minimal.microservices.microservice.contractbaseclasses;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import com.hoffi.minimal.microservices.microservice.MicroserviceApplication;
import com.hoffi.minimal.microservices.microservice.bops.inbound.Sink;
import com.hoffi.minimal.microservices.microservice.bops.inbound.SinkTier1;
import com.hoffi.minimal.microservices.microservice.bops.inbound.SinkTier2;
import com.hoffi.minimal.microservices.microservice.bops.outbound.Source;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier1;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SourceTier2;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

@SpringBootTest(classes = MicroserviceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class DefaultBaseClass {
	
	@Inject
	MessageVerifier messaging;

	@Autowired(required = false)
	Source source;
	@Autowired(required = false)
	SourceTier1 sourceTier1;
	@Autowired(required = false)
	SourceTier2 sourceTier2;

	@Autowired(required = false)
	Sink sink;
	@Autowired(required = false)
	SinkTier1 sinkTier1;
	@Autowired(required = false)
	SinkTier2 sinkTier2;


	@Before
	public void setup() {
		this.messaging.receive("minimal-SourceTo1", 100, TimeUnit.MILLISECONDS);
	}

	protected void methodFromContract() {}
}