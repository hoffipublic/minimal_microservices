package microservice.src.test.resources.contracts

import org.springframework.cloud.contract.spec.Contract

def contractDsl = Contract.make {
    name "contractSourceName"
	label "contractSourceLabel"
	description("""
Contract for a message a `c.h.m.m.m.bops.outbound.Source` sends.
So this contract should be picked up in Contract tests of `@ActiveProfiles({"tier1"}) @SpringBootTest`s
""")
	input {
		triggeredBy('methodFromContract()')
	}
	outputMessage {
		// com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels.OUTPUT
		sentTo("minimal-SourceTo1")
		body('''{ "seq" : "42", "message" : "per contract fromSource", "modifications" : "" }''')
		headers {
			header('ddd', 'testBPDomain')
			header('bp', 'testBProcess')
			header('bpids', '43,44')
			messagingContentType(applicationJson())
		}
	}
}