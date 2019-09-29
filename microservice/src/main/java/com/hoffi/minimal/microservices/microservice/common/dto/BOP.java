// package com.hoffi.minimal.microservices.microservice.common.dto;

// import java.util.Arrays;
// import java.util.LinkedHashSet;
// import java.util.Set;
// import java.util.stream.Collectors;
// import com.hoffi.minimal.microservices.microservice.helpers.ImplementationHint;
// import com.hoffi.minimal.microservices.microservice.helpers.SeqNr;
// import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;

// /**
//  * AS THIS IS A DTO KEEP IT CLEAN OF ANY IMPORTS/DEPENDENCIES THAT CLUTTER SERIALIZATION/DESERIALIZATION!!!
//  * 
//  * struct class identifying a BusinessOPeration.<br/>
//  * Any participating in/outbound business-API/Facade should have <b>one(!) instance</b> of this identifying it, using the
//  * <code>initInitially(...)</code> or <code>initFromUpstream(...)</code> methods.<br/>
//  * Former if it is the initial source of the business flow, latter, if it is a downstream part of one.
//  * While performing an actual businessOperation and probably sending something downstream, use the
//  * createBOP(...) methods of above's <code>initFromUpstream(...)</code> or <code>initInitially(...)</code> instance which then gives a copy of the moduleBOP,
//  * creating a new businessTxId and have a meaningful operation name</br>
//  * 
//  * Be sure to keep the data actually send downstream (especially as tracing baggage) to a minimum.</br>
//  * Remember you always can correlate these informations via the tracings TraceId/SpanId and appName!!!
//  */
// public class BOP extends DTO {
//     private static final long serialVersionUID = 0L;
//     public static final String SEP = "|";
//     public static final String DEFAULTCHUNK = "default";

//     // static informations (does not change throughout the whole inter-process flow)
//     /** usually just one, multiple only if an operation merges multiple incoming businessProcesses */
//     public LinkedHashSet<String> bpIds = new LinkedHashSet<>();
//     public String businessDomain; // think domain driven design (DDD)
//     public String businessProcess; // see it as the clear-text of theTrace
//     // process information (changed as first thing in a process (and only there!) on an incoming flow)
//     public String instanceIndex;
//     public String operation = "<UNSET>"; // set once as first thing on incoming flow within a process
//     // chunk/span information (changed on each "chunk" part done within the flow of one process)
//     public String chunk = DEFAULTCHUNK; // see it as an aggregate clear-text of some Spans

//     /** one time creation at the beginning of a multi-process business operation flow</br>
//      * @param bpIds if left empty generates a single unique one
//      */
//     @ImplementationHint(clazz = TracingHelper.class, comment = "fields have to be also handled for MDC and Baggage")
//     public static BOP initInitially(String businessDomain, String businessProcess, String operation, String instanceIndex, String... bpIds) {
//         BOP bop = new BOP();
//         if (bpIds.length == 0) {
//             String bpId = SeqNr.nextBPId();
//             bop.bpIds.add(bpId);
//         } else {
//             bop.bpIds.addAll(Arrays.asList(bpIds));
//         }
//         bop.businessDomain = businessDomain;
//         bop.businessProcess = businessProcess;
//         // MDC only
//         bop.operation = operation;
//         bop.instanceIndex = "i"+instanceIndex;
//         // leave chunk at default
//         return bop;
//     }
//     /**  creation (at the incoming border of a process flow within each process) */
//     @ImplementationHint(clazz = TracingHelper.class, comment = "fields have to be also handled for MDC and Baggage")
//     public static BOP initFromUpstream(String businessDomain, String businessProcess, String operation, String instanceIndex, String bpIds) {
//         BOP bop = new BOP();
//         bop.bpIds.addAll(Arrays.asList(bpIds.split("\\"+BOP.SEP)));
//         bop.businessDomain = businessDomain;
//         bop.businessProcess = businessProcess;
//         // MDC only
//         bop.operation = operation;
//         bop.instanceIndex = "i"+instanceIndex;
//         // leave chunk at default
//         return bop;
//     }

//     /** private noArgs constructor (needed for jackson json deserialization */
//     protected BOP() {
//     }

//     public BOP newCopy(String chunk) {
//         return newCopy(this.operation, chunk);
//     }
//     public BOP newCopy(String operation, String chunk) {
//         BOP newBop = new BOP();
//         newBop.bpIds.addAll(this.bpIds);
//         newBop.businessDomain = this.businessDomain;
//         newBop.businessProcess = this.businessProcess;
//         newBop.operation = operation;
//         newBop.instanceIndex = this.instanceIndex;
//         newBop.chunk = chunk;
//         return newBop;
//     }

//     // TODO also add to Baggage!!!

//     public LinkedHashSet<String> setBopIds(String ... bpId) {
//         LinkedHashSet<String> oldBopIds = this.bpIds;
//         this.bpIds = new LinkedHashSet<>(Arrays.asList(bpId));
//         return oldBopIds;
//     }

//     public void addBopIds(String... bpId) {
//         this.bpIds.addAll(Arrays.asList(bpId));
//     }

//     public void addBopIds(Set<String> bpIds) {
//         this.bpIds.addAll(bpIds);
//     }


//     public String toStringBopIds() {
//         return bpIds.stream().collect(Collectors.joining(SEP));
//     }

//     public String toStringMod() {
//       return String.format("chunk %s in %s of %s/%s %s", this.chunk, this.operation, this.businessProcess,
//                 this.businessDomain, this.instanceIndex);
//     }

//     public String toString() {
//         return String.format("[%s (%s)]", toStringMod(), toStringBopIds());
//     }
// }
