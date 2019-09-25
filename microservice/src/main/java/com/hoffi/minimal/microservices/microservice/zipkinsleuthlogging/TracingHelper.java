package com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Component
public class TracingHelper {
    private static final Logger log = LoggerFactory.getLogger(TracingHelper.class);

    @Autowired
    private Tracer opentracingTracer;

    public Tracer tracer() { return opentracingTracer; }

    /**
     * ScopedChunk's ScopedBOP maintains a copy!!! of the given BOP <br/>
     * both are autoclosed if they go out of scope on finishOnSpanScopeClose */
    public ScopedChunk startTrace(Span span, BOP bop, String chunkName, boolean finishOnSpanScopeClose) {
        // following makes span active via tracer.scopeManager().activate(span, false);
        // finishing the span and scopedBOP happens on auto .close() if finishOnSpanScopeClose
        ScopedChunk scopedChunk = new ScopedChunk(tracer(), span, bop, chunkName, finishOnSpanScopeClose);
        BOP scopedBOP = scopedChunk.bop();
        tag(MDCKEY.CHUNK.toString() + "Start", scopedBOP.operation);
        setBaggage(BAGGAGEKEY.BUSINESS_PROCESS_IDS, String.join(BOP.SEP, scopedBOP.bpIds));
        setBaggage(BAGGAGEKEY.BUSINESS_DOMAIN, scopedBOP.businessDomain);
        setBaggage(BAGGAGEKEY.BUSINESS_PROCESS_NAME, scopedBOP.businessProcess);
        setMDC(MDCKEY.OPERATION, scopedBOP.operation);
        setMDC(MDCKEY.INSTANCE, scopedBOP.instanceIndex);
        setMDC(MDCKEY.CHUNK, scopedBOP.chunk);
        return scopedChunk;
    }

    public BOP continueTraceFromUpstream(String operationName, String instanceIndex) {
        tag(MDCKEY.CHUNK.toString() + "Start", operationName);
        BOP bop = BOP.initFromUpstream(getBaggage(BAGGAGEKEY.BUSINESS_DOMAIN),
                                       getBaggage(BAGGAGEKEY.BUSINESS_PROCESS_NAME),
                                       operationName, instanceIndex,
                                       getBaggage(BAGGAGEKEY.BUSINESS_PROCESS_IDS));
        // setBaggage(BAGGAGEKEY.BUSINESS_PROCESS_IDS, String.join(BOP.SEP, bop.bpIds));
        // setBaggage(BAGGAGEKEY.BUSINESS_DOMAIN, bop.businessDomain);
        // setBaggage(BAGGAGEKEY.BUSINESS_PROCESS_NAME, bop.businessProcess);
        setMDC(MDCKEY.OPERATION, bop.operation);
        setMDC(MDCKEY.INSTANCE, bop.instanceIndex);
        setMDC(MDCKEY.CHUNK, bop.chunk);

        return bop;
    }

    /** for usage in a try(with resource) statement to start a new sub-Span within the Trace/Span.<br/>
     * ScopedChunk's ScopedBOP maintains a copy!!! of the given BOP<br/>
     * both are autoclosed if they go out of scope on finishOnSpanScopeClose */
    public ScopedChunk startScopedChunk(Span span, BOP bop, String chunkName, boolean finishOnSpanScopeClose) {
        // following makes span active via tracer.scopeManager().activate(span, false);
        // finishing the span and scopedBOP happens  on auto .close() if finishOnSpanScopeClose
        // ScopedChunk's BOP is a copy of given BOP (not the same instance!)
        ScopedChunk scopedChunk = new ScopedChunk(tracer(), span, bop, chunkName, finishOnSpanScopeClose);
        this.startChunk(chunkName);
        return scopedChunk;
    }

    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * ScopedChunk's ScopedBOP maintains a copy!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(BOP bop, String chunkName, boolean finishOnScopeClose) {
        ScopedBOP scopedBOP = new ScopedBOP(bop, chunkName, finishOnScopeClose); // maintains a copy of given BOP
        this.startChunk(chunkName);
        return scopedBOP;
    }

    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * ScopedChunk's ScopedBOP maintains a copy!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(ScopedBOP parentScopedBOP, String chunkName, boolean finishOnScopeClose) {
        ScopedBOP scopedBOP = new ScopedBOP(parentScopedBOP.bop, chunkName, finishOnScopeClose); // maintains a copy of given BOP
        this.startChunk(chunkName);
        return scopedBOP;
    }

    /** for usage in a try(with resource) statement to just start a new Chunk without a new sub-Span<br/>
     * ScopedChunk's ScopedBOP maintains a copy!!! of the given BOP */
    public ScopedBOP startUnscopedChunk(ScopedChunk scopedChunk, String chunkName, boolean finishOnScopeClose) {
        ScopedBOP scopedBOP = new ScopedBOP(scopedChunk.bop(), chunkName, finishOnScopeClose); // maintains a copy of given BOP
        this.startChunk(chunkName);
        return scopedBOP;
    }
    
    private void startChunk(String chunkName) {
        setMDC(MDCKEY.CHUNK, chunkName);
        tag(MDCKEY.CHUNK.toString() + "Start", chunkName);
    }

    /** use this if you want to alter the chunkName within a active Chunk<br/>
      * beware span tagging might become inconsistent upon doing so! */
    public String alterChunk(ScopedChunk scopedChunk, String newChunkName) {
        return alterChunk(scopedChunk.bop(), newChunkName);
    }    
    
    /** use this if you want to alter the chunkName within a active Chunk<br/>
      * beware span tagging might become inconsistent upon doing so! */
      public String alterChunk(ScopedBOP scopedBOP, String newChunkName) {
          return alterChunk(scopedBOP.bop, newChunkName);
      }    
      
    /** use this if you want to alter the chunkName within a active Chunk<br/> 
      * beware span tagging might become inconsistent upon doing so! */
    public String alterChunk(BOP bop, String newChunkName) {
        bop.chunk = newChunkName;
        tag(MDCKEY.CHUNK.toString(), newChunkName);
        return setMDC(MDCKEY.CHUNK, newChunkName);
    }

    public Span tag(String key, String value) {
        return tracer().activeSpan().setTag(key, value);
    }

    public String getBaggage(BAGGAGEKEY key) {
        return getBaggage(tracer().activeSpan(), key);
    }

    public String getBaggage(Span span, BAGGAGEKEY key) {
        if (span == null) {
            return "NOACTIVESPANfor"+key.toString();
        }
        String value = span.getBaggageItem(key.toString());
        if (value == null) {
            return key.toString()+"NOTSET";
        }
        return value;
    }

    public String setBaggage(BAGGAGEKEY key, String value) {
        setBaggage(opentracingTracer.activeSpan(), key, value);
        return value;
    }
    
    public String setBaggage(Span span, BAGGAGEKEY key, String value) {
        if (span == null) {
            log.warn("no active tracing span, cannot set baggage {}={}", key.toString(), value);
        } else {
            span.setBaggageItem(key.toString(), value);
        }
        // to already be able to use in current app (maybe only necessary with opentrace Tracer)
        MDC.put(key.toString(), value);
        return value;
    }

    public String setMDC(MDCKEY key, String value) {
        String formerValue = MDC.get(key.toString());
        MDC.put(key.toString(), value);
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
