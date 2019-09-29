// package com.hoffi.minimal.microservices.microservice.tracing;

// import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import brave.Span;
// import brave.Tracer.SpanInScope;

// /**
//  * Encapsulates<br/>
//  * a) an autoclosable BOP as ScopedBOP and<br/>
//  * b) a trace-Span and its SpanInScope (also autoclosable).<br/>
//  * This is for keeping a BOP(copy) together with the Span(Context) so they are both consistent and
//  * finished correctly when going out of scope
//  */
// public class SpanWithBOP implements AutoCloseable {
//     private static final Logger log = LoggerFactory.getLogger(SpanWithBOP.class);
    
//     public SpanInScope spanInScope;
//     public Span span;
//     private ScopedBOP scopedBOP;
//     public boolean isOperationScope = false;

//     protected SpanWithBOP(Span span, SpanInScope spanInScope, BOP bop, String chunkName) {
//         this.span = span;
//         this.spanInScope = spanInScope;
//         this.scopedBOP = new ScopedBOP(span, bop, chunkName); // maintains a copy of given BOP
//       }

//       public BOP bop() {
//           return scopedBOP.bop;
//       }

//     @Override
//     public void close() throws Exception {
//         log.info("before AUTOCLOSE {} reverting to chunk {} and finishing span {}",
//                 bop().chunk, scopedBOP.oldChunkName, span.context().spanIdString());
//         scopedBOP.close();
//         spanInScope.close(); // but you still have to span.finish() in callers finally {...}
//         log.info("after AUTOCLOSE {} reverting to chunk {}", bop().chunk, scopedBOP.oldChunkName);
//     }
// }