// package com.hoffi.minimal.microservices.microservice.tracing;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
// import io.opentracing.SpanCustomizer;
// import io.opentracing.Tracer;

// @Component
// public class Zipkin {
//     /** convenience Tags not used for logging, just for openzipkin tracing */
//     public enum TAGKEY {
//         MYFANCY("myfancy"), SOMETAG("sometag");

//         private String tag;

//         TAGKEY(String tag) {
//             this.tag = tag;
//         }

//         @Override
//         public String toString() {
//             return tag;
//         }
//     }

//     @Autowired(required = false)
//     private Tracer opentracingTracer;

//     public void tagCurrentSpan(TAGKEY tag, String tagstring) {
//         // Span Tags for
//         SpanCustomizer spanCustomizer = tracer.currentSpanCustomizer();
//         spanCustomizer.tag(tag.toString(), tagstring);
//     }

//     /**
//      * Associates an event that explains latency with the current system time.
//      *
//      * @param value A short annotation indicating the event, like "finagle.retry"
//      */
//     public void annotateCurrentSpan(String annotationForOpenZipkin) {
//         // Span Tags for
//         SpanCustomizer spanCustomizer = tracer.currentSpanCustomizer();
//         spanCustomizer.annotate(annotationForOpenZipkin);
//     }

// }
