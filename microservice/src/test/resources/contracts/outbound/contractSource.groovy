package microservice.src.test.resources.contracts

import org.springframework.cloud.contract.spec.Contract

def contractDsl = Contract.make {
    name "contractSourceName"
	label "contractSourceLabel"
	input {
		triggeredBy('methodFromContract()')
	}
	outputMessage {
		// com.hoffi.minimal.microservices.microservice.bops.channels.SourceChannels.OUTPUT
		sentTo("minimal-SourceTo1")
		body('''{ "seq" : "42", "message" : "per contract fromSource", "modifications" : "" }''')
		headers {
			header('BOOK-NAME', 'foo')
			messagingContentType(applicationJson())
		}
	}
}