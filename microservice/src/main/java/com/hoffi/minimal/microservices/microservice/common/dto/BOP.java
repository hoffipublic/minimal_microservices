package com.hoffi.minimal.microservices.microservice.common.dto;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * struct class identifying a BusinessOperation.<br/>
 * Any in/outbound business-API/Facade should have <b>one(!) instance</b> of this identifying it, using the
 * initModule(...) method.<br/>
 * While performing an actual businessOperation and probably sending something downstream, use the
 * createBOP(...) methods of above's initModule(...) instance which then gives a copy of the moduleBOP,
 * creating a new businessTxId and have a meaningful businessOpName and
 * should be used for this businessOperation for logging/operations/bughunting/demoing ...
 */
public class BOP extends DTO {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger bopIdGenerator = new AtomicInteger();

    public String microservice;
    public String instanceIndex;
    public String businessModule;
    public String bop = "<UNSET>";
    /** in case of an operation that merges multiple incoming business operations */
    public LinkedHashSet<String> bopIds = new LinkedHashSet<>();

    /** creation of a (single) template BOP for e.g. a businessModule */
    public static BOP initModule(String microservice, String instanceIndex, String businessModule) {
        BOP bop = new BOP();
        bop.microservice = microservice;
        bop.instanceIndex = instanceIndex;
        bop.businessModule = businessModule;
        return bop;
    }
    
    private BOP() {
    }

    public BOP copy() {
        BOP bobCopy = new BOP();
        bobCopy.microservice = this.microservice;
        bobCopy.instanceIndex = this.instanceIndex;
        bobCopy.businessModule = this.businessModule;
        bobCopy.bop = this.bop;
        bobCopy.bopIds.addAll(this.bopIds);
        return bobCopy;
    }

    /** create an instance of a dedicated/executed BOP<br/>
     * every executed BOP has a unique ID */
    public BOP createBOP(String businessOperationName) {
        BOP theBop = new BOP();
        theBop.microservice = this.microservice;
        theBop.instanceIndex = this.instanceIndex;
        theBop.businessModule = this.businessModule;
        theBop.bop = businessOperationName;
        theBop.bopIds.add(bopIdGenerator.incrementAndGet() + ""); // every executed BOP has a unique ID
        return theBop;
    }
    
    /** create an instance of a dedicated/executed BOP<br/>
     * every executed BOP has a unique ID */
    public BOP createBOP(String businessModule, String businessOperationName) {
        BOP theBop = createBOP(businessOperationName);
        theBop.businessModule = businessModule;
        return theBop;
    }

    public void addBopIds(String... bopId) {
        this.bopIds.addAll(Arrays.asList(bopId));
    }

    public void addBopIds(Set<String> bopIds) {
        this.bopIds.addAll(bopIds);
    }

    public String toStringBopIds() {
        return bopIds.stream().collect(Collectors.joining("|"));
    }

    public String toString() {
        return String.format("[%s of '%10s/%-15s/%2s' (%s)]", bop, businessModule, microservice,
                instanceIndex, bopIds.stream().collect(Collectors.joining("|")));
    }
}
