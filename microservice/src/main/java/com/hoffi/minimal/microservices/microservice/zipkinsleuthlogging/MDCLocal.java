package com.hoffi.minimal.microservices.microservice.zipkinsleuthlogging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MDCLocal {
    private static final Logger log = LoggerFactory.getLogger(MDCLocal.class);

    public enum MDCKEY {
        CHUNK("chunk");

        private String mdckey;

        MDCKEY(String tag) {
            this.mdckey = tag;
        }

        @Override
        public String toString() {
            return mdckey;
        }
    }


    private MDCLocal() {}

    public static String startChunk(String chunkname) {
        String predChunk;
        try {
            predChunk = MDC.get(MDCKEY.CHUNK.toString());
        } catch (IllegalArgumentException ex) {
            predChunk = "-";
        }
        MDC.put(MDCKEY.CHUNK.toString(), chunkname);
        return predChunk;
    }

    public static void endChunk(String chunkname, String... oldChunkname) {
        if ((oldChunkname != null) && (oldChunkname.length > 0) && (oldChunkname[0] != null)) {
            MDC.put(MDCKEY.CHUNK.toString(), oldChunkname[0]);
        } else {
            try {
                MDC.remove(MDCKEY.CHUNK.toString());
            } catch (IllegalStateException | IllegalArgumentException e) {
                log.error("MDCLocal", e);
            }
        }
    }
}
