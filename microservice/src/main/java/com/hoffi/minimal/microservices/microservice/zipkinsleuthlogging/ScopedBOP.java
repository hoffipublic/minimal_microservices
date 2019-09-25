package com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** only pure BOP instances should be send over the wire to other processes! */
public class ScopedBOP implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ScopedBOP.class);

    public BOP bop;
    public String oldChunkName;
    public boolean autoclose = true;
    public boolean isClosed = false;

    public ScopedBOP(ScopedChunk scopedChunk, String chunkName, boolean finishOnSpanScopeClose) {
        this(scopedChunk.bop(), chunkName, finishOnSpanScopeClose);
    }

    public ScopedBOP(BOP bop, String chunkName, boolean finishOnSpanScopeClose) {
        this.oldChunkName = bop.chunk;
        this.bop = bop.newCopy(chunkName); // scopedBOP.bop.chunk = chunkName;
        this.autoclose = finishOnSpanScopeClose;
    }

    private String finishChunk() {
        String formerChunk = MDC.get(MDCKEY.CHUNK.toString());
        if (!this.bop.chunk.equals(formerChunk)) {
            log.error("current MDC chunk is not the chunk you want to end: current='{}' wanting='{}' setting='{}'",
                    formerChunk, this.bop.chunk, oldChunkName);
        }
        MDC.put(MDCKEY.CHUNK.toString(), this.oldChunkName);
        this.bop.chunk = oldChunkName;
        return formerChunk;
    }
    
    
    @Override
    public void close() throws Exception {
        if(autoclose) {
            log.info("scopedBOP before finishChunk() ...");
            this.finishChunk();
            this.isClosed = true;
            log.info("scopedBOP after finishChunk() ...");

        }
    }

}