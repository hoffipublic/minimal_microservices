package com.hoffi.minimal.microservices.microservice.tracing;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.propagation.ExtraFieldPropagation;

@Component
public class TracingHelper {
    private static final Logger log = LoggerFactory.getLogger(TracingHelper.class);

    public static final String START_BP_PREFIX = "STARTbp_";
    public static final String START_OP_PREFIX = "STARTop_";
    public static final String START_CHUNK_PREFIX = "STARTchunk_";
    public static final String END_BP_PREFIX = "ENDbp_";
    public static final String END_OP_PREFIX = "ENDop_";
    public static final String END_CHUNK_PREFIX = "ENDchunk_";
    public static final String ALT_CHUNK_PREFIX = "ALTchunk_";
    public static final String DOMPROCSEP = "/";

    @Autowired
    private Tracer tracer;

    public Tracer tracer() { return tracer; }

    public Span activeSpan() { return tracer.currentSpan(); }


    public BOP initBOPfromUpstream(String operationName, String instanceIndex) {
        return BOP.initFromUpstream(getBaggage(BAGGAGEKEY.BUSINESS_DOMAIN),
                getBaggage(BAGGAGEKEY.BUSINESS_PROCESS_NAME), operationName, instanceIndex,
                getBaggage(BAGGAGEKEY.BUSINESS_PROCESS_IDS));
    }

    /**
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP <br/>
     * The copy will have the given chunkName as chunk<br/>
     * The chunk the BOP had on calling this method will be stored in ScopedBOP.oldChunkName<br/>
     * If SpanWithBOP goes out of scope (try-with-resource) ScopedBop will be closed and spanInScope finished()<br/>
     * THE CALLER STILL HAS TO FINISH THE SPAN ITSELF IN A FINALLY {...} BLOCK!!!
     */
    public SpanWithBOP startTrace(Span rootSpan, BOP opBOP, String chunkName) {
        annotate(rootSpan, START_BP_PREFIX+ opBOP.businessDomain+DOMPROCSEP+opBOP.businessProcess);
        annotate(rootSpan, START_OP_PREFIX+opBOP.operation);
        setBaggage(rootSpan, BAGGAGEKEY.BUSINESS_PROCESS_IDS, String.join(BOP.SEP, opBOP.bpIds));
        setBaggage(rootSpan, BAGGAGEKEY.BUSINESS_DOMAIN, opBOP.businessDomain);
        setBaggage(rootSpan, BAGGAGEKEY.BUSINESS_PROCESS_NAME, opBOP.businessProcess);
        metaSpanWithBOP(rootSpan, opBOP, chunkName);

        // finishing the spanInScope and scopedBOP happens on auto-.close() of SpanWithBOP
        // but remember you still have to close the span itself with one of tracingHelper.finishSpanXXX()
        SpanInScope spanInScope = tracer.withSpanInScope(rootSpan.start());
        // finishing the span and scopedBOP happens on auto-.close()
        SpanWithBOP spanWithBOP = new SpanWithBOP(rootSpan, spanInScope, opBOP, chunkName);
        spanWithBOP.isOperationScope = true;
        // here scopedBOP within spanWithBOP has new given chunkName, old one is in scopedBOP.oldChunkName for resetting on scopedBOP.close()
        return spanWithBOP;
    }

    /**
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP <br/>
     * The copy will have the default chunkName as chunk<br/>
     * The chunk the BOP had on calling this method will be stored in ScopedBOP.oldChunkName<br/>
     * If SpanWithBOP goes out of scope (try-with-resource) ScopedBop will be closed and spanInScope finished()<br/>
     * THE CALLER STILL HAS TO FINISH THE SPAN ITSELF IN A FINALLY {...} BLOCK!!!
     */
    public SpanWithBOP continueTraceFromUpstream(Span newSpan, BOP opBOP, String chunkName) {
        annotate(newSpan, START_OP_PREFIX+opBOP.operation);
        metaSpanWithBOP(newSpan, opBOP, chunkName);

        // finishing the spanInScope and scopedBOP happens on auto-.close() of SpanWithBOP
        // but remember you still have to close the span itself with one of tracingHelper.finishSpanXXX()
        SpanInScope spanInScope = tracer.withSpanInScope(newSpan.start());
        SpanWithBOP spanWithBOP = new SpanWithBOP(newSpan, spanInScope, opBOP, chunkName);
        spanWithBOP.isOperationScope = true;
        return spanWithBOP;
    }

    /** for usage in a try(with resource) statement to start a new sub-Span within the Trace/Span.<br/>
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP<br/>
     * both are autoclosed if they go out of scope on finishOnScopeClose */
    public SpanWithBOP startSpan(Span span, BOP bop, String chunkName) {
        metaSpanWithBOP(span, bop, chunkName);
        // finishing the spanInScope and scopedBOP happens on auto-.close() of SpanWithBOP
        // but remember you still have to close the span itself with one of tracingHelper.finishSpanXXX()
        SpanInScope spanInScope = tracer.withSpanInScope(span.start());
        SpanWithBOP spanWithBOP = new SpanWithBOP(span, spanInScope, bop, chunkName);

        return spanWithBOP;
    }

    private void metaSpanWithBOP(Span span, BOP parentsBOP, String chunkName) {
        annotate(span, START_CHUNK_PREFIX+chunkName);
        tag(span, MDCKEY.CHUNK.toString(), chunkName);

        tag(span, BAGGAGEKEY.BUSINESS_PROCESS_IDS.toString(), String.join(BOP.SEP, parentsBOP.bpIds));
        tag(span, BAGGAGEKEY.BUSINESS_DOMAIN.toString(), parentsBOP.businessDomain);
        tag(span, BAGGAGEKEY.BUSINESS_PROCESS_NAME.toString(), parentsBOP.businessProcess);
        
        // TODO try using setBaggage for these, once spring.sleuth.local-keys in application.yml will be handled
        setBaggage(span, MDCKEY.OPERATION, parentsBOP.operation);
        setBaggage(span, MDCKEY.CHUNK, chunkName);
        setBaggage(span, MDCKEY.INSTANCE, parentsBOP.instanceIndex);
        
    }


    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(SpanWithBOP parentSpanWithBOP, String chunkName) {
        return startUnscopedChunk(parentSpanWithBOP.bop(), chunkName);
    }
    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(ScopedBOP parentScopedBOP, String chunkName) {
        return startUnscopedChunk(parentScopedBOP.bop, chunkName);
    }
    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * SpanWithBOP's ScopedBOP maintains a newly created COPY/CLONE!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(BOP bop, String chunkName) {
        ScopedBOP scopedBOP = new ScopedBOP(activeSpan(), bop, chunkName); // maintains a copy of given BOP
        // this.startChunk(chunkName);
        setBaggage(activeSpan(), MDCKEY.CHUNK, chunkName);
        tag(activeSpan(), MDCKEY.CHUNK.toString(), chunkName);
        annotate(activeSpan(), START_CHUNK_PREFIX+chunkName);
        return scopedBOP;
    }


    public void finishSpanAndTrace(Span span, BOP bop) {
        annotate(span, END_BP_PREFIX + bop.businessDomain + DOMPROCSEP + bop.businessProcess);
        finishSpanAndOperation(span, null);
    }
    
    public void finishSpanAndOperation(Span span, BOP bop) {
        annotate(span, END_OP_PREFIX + bop.operation);
        setBaggage(span, MDCKEY.OPERATION, bop.operation);
        setBaggage(span, MDCKEY.CHUNK, bop.chunk);
        finishSpan(span, null);
    }

    /** finish span and reset MDC and Baggage to parent Spans Scope (or remove it completely if given bop == null) */
    public void finishSpan(Span span, BOP bop) {
        span.finish();
        // TODO try using setBaggage for these, once spring.sleuth.local-keys in application.yml will be handled
        if (bop != null) {
            setBaggage(span, MDCKEY.OPERATION, bop.operation);
            setBaggage(span, MDCKEY.CHUNK, bop.chunk);
        } else {
            MDC.remove(BAGGAGEKEY.BUSINESS_PROCESS_IDS.toString());
            MDC.remove(BAGGAGEKEY.BUSINESS_DOMAIN.toString());
            MDC.remove(BAGGAGEKEY.BUSINESS_PROCESS_NAME.toString());
            MDC.remove(MDCKEY.OPERATION.toString());
            MDC.remove(MDCKEY.CHUNK.toString());
            MDC.remove(MDCKEY.INSTANCE.toString());
            //System.out.println("XXXXX \n" + MDC.getCopyOfContextMap().keySet().stream().collect(Collectors.joining("\n")));
        }
    }

    public void removeTracingMetaData() {
        try {
            MDC.remove("traceId");
            MDC.remove("spanExportable");
            MDC.remove("spanId");
            MDC.remove("X-Span-Export");
            MDC.remove("X-B3-SpanId");
            MDC.remove("X-B3-TraceId");
            for(BAGGAGEKEY key : BAGGAGEKEY.values()) {
                MDC.remove(key.toString());
            }
            for(MDCKEY key : MDCKEY.values()) {
                MDC.remove(key.toString());
            }
        } catch (RuntimeException re) {

        }
    }


    // private void startChunk(String chunkName) {
    //     setMDC(MDCKEY.CHUNK, chunkName);
    //     tag(MDCKEY.CHUNK.toString() + "Start", chunkName);
    // }

    /** use this if you want to alter the chunkName within a active Chunk<br/>
      * beware span tagging and annotating might become inconsistent upon doing so! */
    public String alterChunk(SpanWithBOP spanWithBOP, String newChunkName) {
        return alterChunk(spanWithBOP.bop(), newChunkName);
    }    

    /** use this if you want to alter the chunkName within a active Chunk<br/>
      * beware span tagging and annotating might become inconsistent upon doing so! */
      public String alterChunk(ScopedBOP scopedBOP, String newChunkName) {
          return alterChunk(scopedBOP.bop, newChunkName);
      }    

    /** use this if you want to alter the chunkName within a active Chunk<br/> 
      * beware span tagging and annotating might become inconsistent upon doing so! */
    public String alterChunk(BOP bop, String newChunkName) {
        tag(MDCKEY.CHUNK.toString(), newChunkName);
        activeSpan().annotate(ALT_CHUNK_PREFIX+bop.chunk+">"+newChunkName);
        bop.chunk = newChunkName;
        return setMDC(MDCKEY.CHUNK, newChunkName);
    }

    public Span annotate(String event) {
        return annotate(activeSpan(), event);
    }
    public Span annotate(Span span, String event) {
        return span.annotate(event);
    }
    public Span tag(String key, String value) {
        return tag(activeSpan(), key, value);
    }
    public Span tag(Span span, String key, String value) {
        return span.tag(key, value);
    }

    public String getBaggage(BAGGAGEKEY key) {
        return getBaggage(activeSpan(), key);
    }
    public String getBaggage(MDCKEY key) {
        return getBaggage(activeSpan(), key);
    }

    public String getBaggage(Span span, BAGGAGEKEY key) {
        return getBaggage(span, key.toString());
    }
    public String getBaggage(Span span, MDCKEY key) {
        return getBaggage(span, key.toString());
    }

    private String getBaggage(Span span, String key) {
        if (span == null) {
            return "NOACTIVESPANfor"+key;
        }
        String value = ExtraFieldPropagation.get(span.context(), key);
        if (value == null) {
            return key+"NOTSET";
        }
        return value;
    }

    // public String setBaggage(BAGGAGEKEY key, String value) {
    //     setBaggage(tracer.activeSpan(), key, value);
    //     return value;
    // }
    
    public String setBaggage(Span span, BAGGAGEKEY key, String value) {
        return setBaggage(span, key.toString(), value);
    }
    public String setBaggage(Span span, MDCKEY key, String value) {
        return setBaggage(span, key.toString(), value);
    }
    private String setBaggage(Span span, String key, String value) {
        setMDC(key, value); // TODO maybe remove if spring.sleuth.local-keys in application.yml will be handled
        if (span == null) {
            log.warn("no active tracing span, cannot set baggage {}={}", key, value);
        } else {
            ExtraFieldPropagation.set(span.context(), key, value);
        }
        // to already be able to use in current app (maybe only necessary with opentrace Tracer)
        // MDC.put(key, value);
        return value;
    }

    public String setMDC(MDCKEY key, String value) {
        return setMDC(key.toString(), value);
    }
    private String setMDC(String key, String value) {
        String formerValue = MDC.get(key);
        MDC.put(key, value);
        return formerValue;
    }

    // public String setMDC(MDCKEY key, String value) {
    //     if (key == MDCKEY.CHUNK) {
    //         String predChunk = MDC.get(MDCKEY.CHUNK.toString());
    //         if (predChunk != null) {
    //             CHUNKS_OF_THREAD.get().push(predChunk);
    //         }
    //         MDC.put(MDCKEY.CHUNK.toString(), value);
    //     } else {
    //         MDC.put(key.toString(), value);
    //     }
    //     return value;
    // }
}
