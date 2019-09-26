package com.hoffi.minimal.microservices.microservice.tracing;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import brave.Span;

/** only pure BOP instances should be send over the wire to other processes! */
public class ScopedBOP implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ScopedBOP.class);

    private Span span;
    public BOP bop;
    public String oldChunkName;
    public boolean isClosed = false;

    public ScopedBOP(SpanWithBOP spanWithBOP, String chunkName) {
        this(spanWithBOP.span, spanWithBOP.bop(), chunkName);
    }

    public ScopedBOP(Span span, BOP bop, String chunkName) {
        this.span = span;
        this.oldChunkName = bop.chunk;
        this.bop = bop.newCopy(chunkName); // scopedBOP.bop.chunk = chunkName;
    }

    private String finishChunk() {
        String formerChunk = MDC.get(MDCKEY.CHUNK.toString());
        if (!this.bop.chunk.equals(formerChunk)) {
            // also happens when you in-process call a new Operation,
            // as tracingHelper.finishSpanAndOperation()
            // wipes out MDC on ending the called Operation
            if (formerChunk == null) {
                log.warn("current MDC chunk is not the chunk you want to end: current='{}' expecting='{}' setting='{}' (maybe you did in-process-call a new starting BusinessOperation?)",
                        formerChunk, this.bop.chunk, this.oldChunkName);
            } else {
                log.error("current MDC chunk is not the chunk you want to end: current='{}' expecting='{}' setting='{}'",
                        formerChunk, this.bop.chunk, this.oldChunkName);
            }
        }
        
        // for unscopedChunks tracingHelper.finishSpanXXX() will NOT be called
        // so we need to reset chunkName in MDC (and local Baggage???)

        // // tracingHelper.setBaggage(span, MDCKEY.CHUNK, chunkName);
        MDC.put(MDCKEY.OPERATION.toString(), this.bop.operation);
        MDC.put(MDCKEY.CHUNK.toString(), this.oldChunkName); // TODO maybe remove if spring.sleuth.local-keys in application.yml will be handled
        if (span != null) {
            // ExtraFieldPropagation.set(span.context(), MDCKEY.CHUNK.toString(), this.oldChunkName);
            span.annotate(TracingHelper.END_CHUNK_PREFIX+this.bop.chunk);
        } else {
            log.warn("span is null for {} former: {}", this.bop.chunk, formerChunk);
        }
        this.bop.chunk = oldChunkName;
        return formerChunk;
    }
    
    
    @Override
    public void close() throws Exception {
        this.finishChunk();
        this.isClosed = true;
    }

}