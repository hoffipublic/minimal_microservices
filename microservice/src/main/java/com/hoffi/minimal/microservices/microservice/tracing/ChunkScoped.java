package com.hoffi.minimal.microservices.microservice.tracing;

import java.io.Closeable;
import java.io.IOException;
import org.slf4j.MDC;
import brave.Span;

public class ChunkScoped implements Closeable {

    private TracingHelper tracingHelper;
    private Span span;
    public String chunkName;
    private boolean spanAnnotateEndChunk;
    public String formerChunkName;

    /** has to be created before given span.start() to determine chunkName of parent = activeSpan() */
    public ChunkScoped(TracingHelper tracingHelper, Span span, String chunkName, boolean spanAnnotateEndChunk) {
        this.tracingHelper = tracingHelper;
        this.span = span;
        this.chunkName = chunkName;
        this.spanAnnotateEndChunk = spanAnnotateEndChunk;
        this.formerChunkName = tracingHelper.getBaggage(tracingHelper.activeSpan(), MDCKEY.CHUNK);
    }

    /** close of ChunkScoped (if used within a new childSpan) has to be called AFTER childSpan.finish() */
    @Override
    public void close() throws IOException {
        // close of ChunkScoped (if used within a new childSpan) has to be called AFTER childSpan.finish()
        if(spanAnnotateEndChunk) {
            tracingHelper.annotate(span, TracingHelper.END_CHUNK_PREFIX + chunkName);
            tracingHelper.setBaggage(tracingHelper.activeSpan(), MDCKEY.CHUNK, formerChunkName);
            MDC.put( MDCKEY.CHUNK.toString(), formerChunkName);
        }
    }

}