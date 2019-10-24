// package microservice.src.test.resources.contracts

// import org.springframework.cloud.contract.spec.Contract

// def contractDsl = Contract.make {
//     name "contractTier1InboundName"
// 	label "contractTier1InboundLabel"
// 	input {
// 		messageFrom('minimal-SourceTo1')
// 		messageBody('''{ "seq" : "42", "message" : "fromSource", "modifications" : "" }''')
// 		messageHeaders {
// 			header('sample', 'header')
// 		}
// 	}
// 	outputMessage {
// 		sentTo('minimal-1To2')
// 		body('''{ "seq" : "42", "message" : "fromTier1", "modifications" : "transformed by tier1" }''')
// 		headers {
// 			header('BOOK-NAME', 'foo')
// 		}
// 	}
// }