package com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * encapsulates a ScopedBOP and a tracing scope within an AutoClosable, so they are both finished
 * correctly when going out of scope
 */
public class ScopedChunk implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ScopedChunk.class);
    
    public Scope scope;
    private ScopedBOP scopedBOP;
    private boolean finishOnSpanScopeClose;

    protected ScopedChunk(Tracer opentracingTracer, Span span, BOP bop, String chunkName, boolean finishOnSpanScopeClose) {
        this.finishOnSpanScopeClose = finishOnSpanScopeClose;
        this.scope = opentracingTracer.scopeManager().activate(span, true); // handled in this.close()
        this.scopedBOP = new ScopedBOP(bop, chunkName, finishOnSpanScopeClose); // maintains a copy of given BOP
      }

      public BOP bop() {
          return scopedBOP.bop;
      }

    @Override
    public void close() throws Exception {
        if(finishOnSpanScopeClose) {
            log.info("AUTOCLOSE {} reverting to chunk {} ", bop().chunk, scopedBOP.oldChunkName);
            scopedBOP.close();
            scope.close();
        }
    }

}