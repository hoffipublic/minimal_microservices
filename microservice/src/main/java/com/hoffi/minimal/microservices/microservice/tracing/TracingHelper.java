package com.hoffi.minimal.microservices.microservice.tracing;

import java.util.LinkedHashSet;

import brave.baggage.BaggageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;

@Component
public class TracingHelper {
    private static final Logger log = LoggerFactory.getLogger(TracingHelper.class);

    public static final String DEFAULTCHUNKNAME = "default";
    public static final String START_BP_PREFIX = "STARTbp_";
    public static final String START_OP_PREFIX = "STARTop_";
    public static final String START_CHUNK_PREFIX = "STARTchunk_";
    public static final String END_BP_PREFIX = "ENDbp_";
    public static final String END_OP_PREFIX = "ENDop_";
    public static final String END_CHUNK_PREFIX = "ENDchunk_";
    public static final String ALT_CHUNK_PREFIX = "ALTchunk_";
    public static final String DOMPROCSEP = "/";
    public static final String BPIDSSEP = "|";

    private BaggageField BAGGAGE_BUSINESS_DOMAIN;
    private BaggageField BAGGAGE_BUSINESS_PROCESS_NAME;
    private BaggageField BAGGAGE_BUSINESS_PROCESS_IDS;
    private BaggageField BAGGAGE_INSTANCE;
    private BaggageField BAGGAGE_OPERATION;
    private BaggageField BAGGAGE_CHUNK;
    public TracingHelper() {
        BAGGAGE_BUSINESS_DOMAIN = BaggageField.create(BAGGAGE_REMOTE_KEY.BUSINESS_DOMAIN.toString());
        BAGGAGE_BUSINESS_PROCESS_NAME = BaggageField.create(BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_NAME.toString());
        BAGGAGE_BUSINESS_PROCESS_IDS = BaggageField.create(BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_IDS.toString());
        BAGGAGE_OPERATION = BaggageField.create(BAGGAGE_LOCAL_KEY.OPERATION.toString());
        BAGGAGE_CHUNK = BaggageField.create(BAGGAGE_LOCAL_KEY.CHUNK.toString());
        BAGGAGE_INSTANCE = BaggageField.create(BAGGAGE_LOCAL_KEY.INSTANCE.toString());
        BAGGAGE_BUSINESS_DOMAIN.updateValue("INIT");
        BAGGAGE_BUSINESS_PROCESS_NAME.updateValue("INIT");
        BAGGAGE_BUSINESS_PROCESS_IDS.updateValue("INIT");
        BAGGAGE_OPERATION.updateValue("INIT");
        BAGGAGE_CHUNK.updateValue("INIT");
        BAGGAGE_INSTANCE.updateValue("INIT");
    }

    @Autowired
    private Tracer tracer;

    public Tracer tracer() { return tracer; }

    public Span activeSpan() { return tracer.currentSpan(); }

    /**
     * Returns a new child span if there's a {@link #currentSpan()} or a new trace if there isn't<br/>
     * for usage in a try(ScopedSpan = scopedSpan tracingHelper.startSpan(...)) { <try-with-resource-block>; } finally { tracingHelper.finishXXX(); }
     * <br/>
     * {@link #startSpan(Span, String)}
     * <p>
     * returned Span has to be finished in finally block
     *
     * <p>
     * Prefer {@link #startScopedSpan(String)} if you are tracing a synchronous function or code
     * block.
     */
    public Span nextSpan(String spanName) {
        return tracer.nextSpan().name(spanName);
    }

    /**
     * Explicitly creates a new trace. The result will be a root span (no parent span ID).
     * <p>
     * To implicitly create a new trace, or a span within an existing one, use {@link #nextSpan()}.
     */
    public Span newTrace(String traceName) {
        return tracer.newTrace().name(traceName);
    }


    /**
     * Use within a try(withResource) { block }!!!<br/>
     * <br/>
     * SpanScoped contains:<br/>
     * a) the (started) Span<br/>
     * b) the (started) ChunkScoped with current and new chunkName (Autoclose on scope finish)<br/>
     * c) the (started) Tracer.SpanInScope (Autoclosing on scope finish, but not autoclosing the
     * Span itself) <br/>
     * on end of try(withResource) { block } Tracer.SpanInScop and ShunkScoped will be
     * AutoClosable.close()<br/>
     * <br/>
     * CALLER STILL HAS TO FINISH THE SPAN ITSELF IN A finally {...} BLOCK!!!
     * 
     * @return SpanScoped containig the now started Span
     */
    public SpanScoped startTrace(Span rootSpan, String businessDomain, String businessProcess, String bpIds, String operation, String instanceIndex) {
        LinkedHashSet<String> bpIdsSet = bpIdsToSet(bpIds);
        return startTrace(rootSpan, businessDomain, businessProcess, bpIdsSet, operation, instanceIndex);
    }
    public SpanScoped startTrace(Span rootSpan, String businessDomain, String businessProcess, LinkedHashSet<String> bpIdsSet, String operation, String instanceIndex) {
        String bpIds = bpIdsToString(bpIdsSet);
        annotate(rootSpan, START_BP_PREFIX+ businessDomain+DOMPROCSEP+businessProcess);
        annotate(rootSpan, START_OP_PREFIX+operation);
        setBaggage(rootSpan, businessDomain, businessProcess, bpIds);
        setLocalBaggage(rootSpan, operation, DEFAULTCHUNKNAME, instanceIndex);
        setTagsAndAnnotations(rootSpan, businessDomain, businessProcess, bpIds, operation, operation);

        // finishing the spanInScope within SpanScoped happens on auto-.close()
        // but remember you still have to close the span itself with one of tracingHelper.finishSpanXXX()
        SpanInScope spanInScope = tracer.withSpanInScope(rootSpan.start());
        // finishing the Tracer.SpanInScop and ShunkScoped happens on AutoClosable.close() of SpanScoped
        SpanScoped spanScoped = new SpanScoped(rootSpan, spanInScope, null);
        return spanScoped;
    }

    /**
     * If SpanScoped goes out of scope (try-with-resource) its SpanInScoped will be closed<br/>
     * just wrapping the sleuth specifics away from business logic and business operations
     * THE CALLER STILL HAS TO FINISH THE SPAN ITSELF IN A FINALLY {...} BLOCK!!!
     */
    public Span continueTraceFromUpstream(String operationName, String instanceIndex) {
        activeSpan().name(operationName);
        annotate(activeSpan(), START_OP_PREFIX+operationName);
        // TODO
        // MDC.put neccessary as it will not be put into MDC by sleuth before tracer.withSpanInScope(span.start());
        MDC.put(BAGGAGE_LOCAL_KEY.OPERATION.toString(), operationName);
        MDC.put(BAGGAGE_LOCAL_KEY.CHUNK.toString(), DEFAULTCHUNKNAME);
        MDC.put(BAGGAGE_LOCAL_KEY.INSTANCE.toString(), instanceIndex);
        setLocalBaggage(activeSpan(), operationName, DEFAULTCHUNKNAME, instanceIndex);
        setTagsAndAnnotations(activeSpan(), 
                getBaggage(BAGGAGE_REMOTE_KEY.BUSINESS_DOMAIN),
                getBaggage(BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_NAME),
                getBaggage(BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_IDS), operationName, DEFAULTCHUNKNAME);
        return activeSpan();
    }


    private void setBaggage(Span span, String businessDomain, String businessProcess, String bpIds) {
        setBaggage(span, BAGGAGE_REMOTE_KEY.BUSINESS_DOMAIN, businessDomain);
        setBaggage(span, BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_NAME, businessProcess);
        setBaggage(span, BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_IDS, bpIds);
    }

    public void setLocalBaggage(Span span, String operation, String chunkName, String instanceIndex) {
        setBaggage(span, BAGGAGE_LOCAL_KEY.OPERATION, operation);
        setBaggage(span, BAGGAGE_LOCAL_KEY.CHUNK, chunkName);
        setBaggage(span, BAGGAGE_LOCAL_KEY.INSTANCE, instanceIndex);
    }

    private void setTagsAndAnnotations(Span span, String businessDomain, String businessProcess, String bpIds, String operationName, String chunkName) {
        // if(!chunkName.equals(operationName)) {
        //     annotate(span, START_CHUNK_PREFIX+chunkName);
        // }
        // tag(span, BAGGAGE_LOCAL_KEY.CHUNK.toString(), chunkName);

        tag(span, BAGGAGE_REMOTE_KEY.BUSINESS_DOMAIN.toString(), businessDomain);
        tag(span, BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_NAME.toString(), businessProcess);
        tag(span, BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_IDS.toString(), bpIds);
    }

    /**
     * /** Use within a try(withResource) { block }!!!<br/>
     * <br/>
     * SpanScoped contains:<br/>
     * a) the (started) Span<br/>
     * b) the (started) ChunkScoped with current and new chunkName (Autoclose on scope finish)<br/>
     * c) the (started) Tracer.SpanInScope (Autoclosing on scope finish, but not autoclosing the
     * Span itself) <br/>
     * on end of try(withResource) { block } Tracer.SpanInScop and ShunkScoped will be
     * AutoClosable.close()<br/>
     * <br/>
     * CALLER STILL HAS TO FINISH THE SPAN ITSELF IN A finally {...} BLOCK!!!
     * 
     * @return the SpanScoped passed to this method as parameter, but with SpanInScope set, containig the now started Span
     */
    public SpanScoped startSpan(Span span, String spanName) {
        // ChunkScoped will remember the activeSpan()'s chunkName
        ChunkScoped chunkScoped = new ChunkScoped(this, span, spanName, false); // remember former chunkName for AutoClosable.close()
        setBaggage(span, BAGGAGE_LOCAL_KEY.CHUNK, spanName);
        
        // finishing the Tracer.SpanInScope and ChunkScoped within SpanScoped on AutoCloseable.close()
        // but remember you still have to close the span itself with one of tracingHelper.finishSpanXXX()
        SpanInScope spanInScope = tracer.withSpanInScope(span.start());
        // SpanScoped.chunkScoped will remember the activeSpan()'s chunkName
        SpanScoped spanScoped = new SpanScoped(span, spanInScope, chunkScoped);
        return spanScoped;
    }


    /** starts a chunk in the currently activeSpan() <br/> 
     * for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * {@link ChunkScoped} will remember the old chunkname and reset it on AutoClosable.close() */
    public ChunkScoped startChunk(String chunkName) {
        ChunkScoped chunkScoped = new ChunkScoped(this, activeSpan(), chunkName, true); // remember former chunkName for AutoClosable.close()
        annotate(activeSpan(), START_CHUNK_PREFIX+chunkName);
        setBaggage(activeSpan(), BAGGAGE_LOCAL_KEY.CHUNK, chunkName);
        MDC.put(BAGGAGE_LOCAL_KEY.CHUNK.toString(), chunkName);
        tag(activeSpan(), BAGGAGE_LOCAL_KEY.CHUNK.toString(), chunkName);
        return chunkScoped;
    }


    public void finishSpanAndTrace(Span span, String operation) {
        annotate(span, END_BP_PREFIX + getBaggage(span, BAGGAGE_REMOTE_KEY.BUSINESS_DOMAIN) + DOMPROCSEP
                + getBaggage(span, BAGGAGE_REMOTE_KEY.BUSINESS_PROCESS_NAME));
        finishSpanAndOperation(span, operation);
    }
    
    public void finishSpanAndOperation(Span span, String operation) {
        annotate(span, END_OP_PREFIX + operation);
        finishSpan(span);
    }

    /** finish span and reset MDC and Baggage to parent Spans Scope (or remove it completely if given bop == null) <br/>
     * BAGGAGE_LOCAL_KEY.CHUNK should be set in ChunkScoped.close() on AutoClosable.close() for UnscopedChunks */
    public void finishSpan(Span span) {
        span.finish();
        //setBaggage(activeSpan(), BAGGAGE_LOCAL_KEY.CHUNK, oldChunkName);
    }

    public void alterChunk(SpanScoped spanScoped, String newChunkName) {
        alterChunk(spanScoped.chunkScoped, newChunkName);
    }

    /** use this if you want to alter the chunkName within a active Chunk<br/> 
      * beware span tagging and annotating might become inconsistent upon doing so! */
    public void alterChunk(ChunkScoped chunkedScope, String newChunkName) {
        tag(BAGGAGE_LOCAL_KEY.CHUNK.toString(), newChunkName);
        activeSpan().annotate(ALT_CHUNK_PREFIX+getBaggage(BAGGAGE_LOCAL_KEY.CHUNK)+">"+newChunkName);
        setBaggage(BAGGAGE_LOCAL_KEY.CHUNK, newChunkName); // this will not be send to zipkin before the span finishes
        MDC.put(BAGGAGE_LOCAL_KEY.CHUNK.toString(), newChunkName);
    }

    public Span annotate(Span span, String event) {
        if (span != null) {
            return span.annotate(event);
        } else {
            log.warn("cannot annotate as span is null for event: '{}'", event);
            return null;
        }
    }
    public Span tag(String key, String value) {
        return tag(activeSpan(), key, value);
    }
    public Span tag(Span span, String key, String value) {
        if (span != null) {
            return span.tag(key, value);
        } else {
            log.warn("cannot tag as span is null for key/value '{}/{}'", key, value);
            return null;
        }
    }



    public String getBaggage(BAGGAGE_REMOTE_KEY key) {
        return getBaggage(activeSpan(), key);
    }
    public String getBaggage(BAGGAGE_LOCAL_KEY key) {
        return getBaggage(activeSpan(), key);
    }
    public String getBaggage(Span span, BAGGAGE_REMOTE_KEY key) {
        return getBaggage(span, getBaggageField(key));
    }
    public String getBaggage(Span span, BAGGAGE_LOCAL_KEY key) {
        return getBaggage(span, getBaggageField(key));
    }

    private BaggageField getBaggageField(BAGGAGE_REMOTE_KEY key) {
        switch(key) {
            case BUSINESS_DOMAIN:       return BAGGAGE_BUSINESS_DOMAIN;
            case BUSINESS_PROCESS_NAME: return BAGGAGE_BUSINESS_PROCESS_NAME;
            case BUSINESS_PROCESS_IDS:  return BAGGAGE_BUSINESS_PROCESS_IDS;
            default: throw new RuntimeException("no such " + BAGGAGE_REMOTE_KEY.class.getSimpleName() + " field");
        }
    }
    private BaggageField getBaggageField(BAGGAGE_LOCAL_KEY key) {
        switch(key) {
            case OPERATION: return BAGGAGE_OPERATION;
            case CHUNK:     return BAGGAGE_CHUNK;
            case INSTANCE:  return BAGGAGE_INSTANCE;
            default: throw new RuntimeException("no such " + BAGGAGE_REMOTE_KEY.class.getSimpleName() + " field");
        }
    }

    private String getBaggage(Span span, BaggageField field) {
        if (span == null) {
            return "NOACTIVESPANfor"+field.name();
        }
        String value = field.getValue(span.context());
        if (value == null) {
            return field.name()+"NOTSET";
        }
        return value;
    }

    // public String setBaggage(BAGGAGEKEY key, String value) {
    //     setBaggage(tracer.activeSpan(), key, value);
    //     return value;
    // }

    public void setBaggage(BAGGAGE_REMOTE_KEY key, String value) {
        setBaggage(activeSpan(), key, value);
    }
    public void setBaggage(BAGGAGE_LOCAL_KEY key, String value) {
        setBaggage(activeSpan(), key, value);
    }
    public void setBaggage(Span span, BAGGAGE_REMOTE_KEY key, String value) {
        setBaggage(span, getBaggageField(key), value);
    }
    public void setBaggage(Span span, BAGGAGE_LOCAL_KEY key, String value) {
        setBaggage(span, getBaggageField(key), value);
    }
    private void setBaggage(Span span, BaggageField field, String value) {
        if (span == null) {
            log.warn("no active tracing span, cannot set baggage {}={}", field.name(), value);
        } else {
            field.updateValue(span.context(), value);
        }
    }

    public LinkedHashSet<String> bpIdsToSet(String bpIds) {
        LinkedHashSet<String> bpIdsSet = new LinkedHashSet<>();
        for (String bpId : bpIds.split("//s*,//s*")) {
            bpIdsSet.add(bpId);
        }
        return bpIdsSet;
    }

    public String bpIdsToString(LinkedHashSet<String> bpIdsSet) {
        return String.join(BPIDSSEP, bpIdsSet);
    }

    public void reportException(Throwable t) {
        reportException(activeSpan(), t);
    }
    public void reportException(Span opSpan, Throwable t) {
        opSpan.annotate(String.format("Exception: in operation: %s parentChunk: %s Exception: %s",
                getBaggage(opSpan, BAGGAGE_LOCAL_KEY.OPERATION),
                getBaggage(opSpan, BAGGAGE_LOCAL_KEY.CHUNK),
                t.getMessage()));

    }
}
