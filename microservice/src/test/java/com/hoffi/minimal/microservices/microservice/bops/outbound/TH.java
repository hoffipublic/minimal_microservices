package com.hoffi.minimal.microservices.microservice.bops.outbound;

import com.hoffi.minimal.microservices.microservice.common.dto.BOP;
import com.hoffi.minimal.microservices.microservice.common.dto.DTOTestHelper;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;

public class TH {

    public static final String REF_BOBID = "42";
    public static final String REF_BOB_DOMAIN = "myCurrentBDomain";
    public static final String REF_BOB_PROCESS = "myBProcessName";
    public static final String REF_BOB_INSTANCE = "i0";
    public static final String REF_BOB_INSTANCE_PLAIN = "0";
    public static final String REF_BOB_OPERATION = "timerMessageSource";
    public static final String REF_BOB_CHUNK = "testChunk";
    public static final String REF_BOB_CHUNK_TIMER = "timerSend";

    public static final Integer REF_MESSAGE_SEQ = 5;
    public static final String REF_MESSAGE = "refMessage";
    public static final String REF_MESSAGE_TIMER = "fromSource";
    public static final String REF_MESSAGE_NEW = "fromTest";
    public static final String REF_MODIFICATION = "";

    private TH() {}

    public static String inc(String integer) {
        return Integer.valueOf(Integer.parseInt(integer) + 1).toString();
    }

    public static MessageDTO referenceMessageDTO() {
        // MessageDTO with BOP we use to compare the received message with
        BOP referenceBOP = DTOTestHelper.getPlainBOP(); // as default constructor is
                                                        // private/protected
        referenceBOP.bpIds.add(REF_BOBID);
        referenceBOP.businessDomain = REF_BOB_DOMAIN;
        referenceBOP.instanceIndex = REF_BOB_INSTANCE;
        referenceBOP.operation = REF_BOB_OPERATION;
        referenceBOP.businessProcess = REF_BOB_PROCESS;
        referenceBOP.chunk = REF_BOB_CHUNK;
        MessageDTO referenceMessageDTO = DTOTestHelper.getPlainMessageDTO(); // as default
                                                                             // constructor is
                                                                             // private/protected
        referenceMessageDTO.seq = REF_MESSAGE_SEQ;
        referenceMessageDTO.bop = referenceBOP;
        referenceMessageDTO.bops.add(referenceBOP);
        referenceMessageDTO.message = REF_MESSAGE;
        referenceMessageDTO.modifications = REF_MODIFICATION;
        return referenceMessageDTO;
    }

}