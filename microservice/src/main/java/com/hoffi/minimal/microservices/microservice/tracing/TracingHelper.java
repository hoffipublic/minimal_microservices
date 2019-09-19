package com.hoffi.minimal.microservices.microservice.tracing;

import io.opentracing.Span;
import io.opentracing.Tracer;

public class TracingHelper {
    public static Span traceCreateSpan(Tracer tracer, String spanName) {
        Span span = tracer.buildSpan(spanName).start();
        return span;
    }

    /** @deprecated should be used with
     * <code>
        Span span = tracer.buildSpan("...").start();
        try (Scope scope = tracer.activateSpan(span)) {
        } catch (Exception e) {
            span.log(...); // Report any errors properly.
        } finally {
            span.finish(); // Optionally close the Span.
        }
     * </code
     */
    @Deprecated
    public static void traceFinishSpan(Tracer tracer) {
        Span currentSpan = tracer.activeSpan();
        currentSpan.finish();
    }

    public static Span traceTag(Tracer tracer, String tagKey, String tagValue) {
        Span currentSpan = tracer.activeSpan();
        if (currentSpan == null) {
            // should only happen in testing context
            currentSpan = tracer.buildSpan("testSpan").start();
        }
        currentSpan.setTag(tagKey, tagValue);
        return currentSpan;
    }
}