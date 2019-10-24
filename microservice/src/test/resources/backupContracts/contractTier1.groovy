// package microservice.src.test.resources.contracts

// import org.springframework.cloud.contract.spec.Contract

// def contractDsl = Contract.make {
//     name "contractTier1OutboundName"
// 	label "contractTier1OutboundLabel"
// 	input {
// 		triggeredBy('methodFromContract()')
// 	}
// 	outputMessage {
// 		sentTo('minimal-1To2')
// 		body('''{ "seq" : "42", "message" : "fromTier1", "modifications" : "" }''')
// 		headers {
// 			header('BOOK-NAME', 'foo')
// 			messagingContentType(applicationJson())
// 		}
// 	}
// }