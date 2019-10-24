// package microservice.src.test.resources.contracts

// import org.springframework.cloud.contract.spec.Contract

// def contractDsl = Contract.make {
//     name "contractSinkInboundName"
// 	label "contractSinkInboundLabel"
// 	input {
// 		messageFrom('minimal-2ToSink')
// 		messageBody('''{ "seq" : "42", "message" : "fromTier2", "modifications" : "" }''')
// 		messageHeaders {
// 			header('sample', 'header')
// 		}
// 		assertThat('methodFromContract()')
// 	}
// }