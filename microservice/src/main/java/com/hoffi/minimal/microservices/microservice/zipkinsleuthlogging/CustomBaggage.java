// package com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging;

// import org.slf4j.MDC;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
// import io.opentracing.Span;
// import io.opentracing.Tracer;
// import io.opentracing.Tracer.SpanBuilder;

// @Component
// public class CustomBaggage {

//     /**
//      * all keys have to be defined in application.properties
//      * spring.sleuth.(baggage-keys|propagation-keys)
//      */
//     public enum BAGGAGEKEY {
//         /* static baggage = stay the same throughout the whole trace */
//         BUSINESS_PROCESS_NAME("bpn"), BUSINESS_PROCESS_ID("bpid"),

//         /* dynamic baggage = may change throughout the whole trace */
//         SUCCESSOR_PROCESS("succ");

//         private String baggagekey;

//         BAGGAGEKEY(String baggagekey) {
//             this.baggagekey = baggagekey;
//         }

//         @Override
//         public String toString() {
//             return baggagekey;
//         }
//     }

//     @Autowired(required = false)
//     private Tracer tracer;

//     /**
//      * set static and dynamic keys Baggage as defined in application.yml
//      * spring.sleuth.(baggage-keys|propagation-keys)
//      */
//     public void startTrace(String businessProcessName, String businessProcessId,
//             String successorProcesses) {
//         Span currentSpan = tracer.activeSpan();
//         SpanBuilder traceContext;
//         if (currentSpan == null) {
//             // should only happen in testing context
//             currentSpan = tracer.buildSpan("testSpan").start();
//         }
//         // set static and dynamic keys Baggage as defined in application.yml spring.sleuth.(baggage-keys|propagation-keys)
//         /* static baggage = stay the same throughout the whole trace */
//         ExtraFieldPropagation.set(traceContext, BAGGAGEKEY.BUSINESS_PROCESS_NAME.toString(), businessProcessName);
//         ExtraFieldPropagation.set(traceContext, BAGGAGEKEY.BUSINESS_PROCESS_ID.toString(), businessProcessId);
//         MDC.put(BAGGAGEKEY.BUSINESS_PROCESS_NAME.toString(), businessProcessName);
//         MDC.put(BAGGAGEKEY.BUSINESS_PROCESS_ID.toString(), businessProcessId);
//         // tag sleuth spans with static baggage
//         SpanCustomizer spanCustomizer = tracer.currentSpanCustomizer();
//         spanCustomizer.tag(BAGGAGEKEY.BUSINESS_PROCESS_NAME.toString(), businessProcessName);
//         spanCustomizer.tag(BAGGAGEKEY.BUSINESS_PROCESS_ID.toString(), businessProcessId);
//         /* dynamic baggage = may change throughout the whole trace */
//         dynBaggageTagSuccessorProcess(successorProcesses);
//     }

//     public void dynBaggageTagSuccessorProcess(String successorProcesses) {
//         TraceContext traceContext = tracer.currentSpan().context();
//         ExtraFieldPropagation.set(traceContext, BAGGAGEKEY.SUCCESSOR_PROCESS.toString(), successorProcesses);
//         MDC.put(BAGGAGEKEY.SUCCESSOR_PROCESS.toString(), successorProcesses); // put in MDC immediately so it appears in the following log messages of this same thread
//         SpanCustomizer spanCustomizer = tracer.currentSpanCustomizer();
//         spanCustomizer.tag(BAGGAGEKEY.SUCCESSOR_PROCESS.toString(), successorProcesses);

//     }

//     public String get(BAGGAGEKEY key) {
//         TraceContext traceContext = tracer.currentSpan().context();
//         return ExtraFieldPropagation.get(traceContext, key.toString());
//     }
// }
