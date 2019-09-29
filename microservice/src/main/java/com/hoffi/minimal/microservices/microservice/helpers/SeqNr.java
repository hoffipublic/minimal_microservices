package com.hoffi.minimal.microservices.microservice.helpers;

import java.util.concurrent.atomic.AtomicInteger;

public class SeqNr {

    private static AtomicInteger bpIdGenerator = new AtomicInteger();
    private static AtomicInteger messageSeqNrGenerator = new AtomicInteger();

    public static String nextBPId() {
        return bpIdGenerator.incrementAndGet() + "";
    }

    public static String nextSeqNr() {
        return messageSeqNrGenerator.incrementAndGet() + "";
    }

    private SeqNr() {}
}