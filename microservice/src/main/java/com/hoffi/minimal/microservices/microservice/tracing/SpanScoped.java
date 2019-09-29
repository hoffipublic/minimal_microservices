package com.hoffi.minimal.microservices.microservice.tracing;

import java.io.Closeable;
import java.io.IOException;
import brave.Span;
import brave.Tracer.SpanInScope;

public class SpanScoped implements Closeable {

    public Span span;
    public ChunkScoped chunkScoped; // for annotating old Span with chunk specific things
    public SpanInScope spanInScope;

    /**
     * 
     * @param span the newSpan to activate
     * @param spanInScope the spanInScope after that span was .start()ed
     * @param chunkScoped the ChunkScoped before that given span was .start()ed
     */
    public SpanScoped(Span span, SpanInScope spanInScope, ChunkScoped chunkScoped) {
        this.span = span;
        this.spanInScope = spanInScope;
        this.chunkScoped = chunkScoped;
    }

    SpanScoped start() {
        return this;
    }

    @Override
    public void close() throws IOException {
        spanInScope.close();
        // former parent Span has to be active again here,
        // so chunkScoped will set the oldChunkName to the correct Span
        if(chunkScoped != null) {
            chunkScoped.close();
        }
    }
}