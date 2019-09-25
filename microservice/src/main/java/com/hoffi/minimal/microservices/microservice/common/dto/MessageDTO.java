package com.hoffi.minimal.microservices.microservice.common.dto;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

/** a DataTransferObject that may be modified by the same microservice or microservices downstream<br/>
 * on method <code>create(...)</code> id is counted up from 1 within each running process (ONLY by static <code>create(...)</code> method)<br/>
 * followup operations by the same microservice or downstream microservices should ONLY use the <code>transform(...)</code> method<br/>
 * which sets a new message, adds modification metadata, adds a BOP and keeps the id
 */
public class MessageDTO extends DTO {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger seqGenerator = new AtomicInteger(0);

    private enum CREATEMODE {
        NEW, CLONE
    };

    public static MessageDTO create(BOP bop) {
        return new MessageDTO(bop, CREATEMODE.NEW);
    }

    public static MessageDTO create(BOP bop, String message, String modifications) {
        return new MessageDTO(bop, message, modifications, CREATEMODE.NEW);
    }

    public Integer seq = Integer.valueOf(-1);
    public BOP bop;
    public LinkedList<BOP> bops = new LinkedList<>(); // only for demoing purposes
    /** set to new content on any downstream microservice transformation on it */
    public String message = "<initial>";
    /** appended to (semicolon separated) on any downstream microservice transformation on it */
    public String modifications = "";


    /** private noArgs constructor (needed for jackson json deserialization */
    protected MessageDTO() {}

    /** private constructor */
    private MessageDTO(BOP bop, CREATEMODE mode) {
        Assert.notNull(bop, "bop was null");
        if (mode == CREATEMODE.NEW) {
            seq = seqGenerator.incrementAndGet();
            bops.add(bop); // initial/creating bop
        }
        this.bop = bop;
    }

    /** private constructor */
    private MessageDTO(BOP bop, String message, String modification, CREATEMODE mode) {
        this(bop, mode);
        Assert.notNull(modification, "modification was null");
        this.message = message;
        this.modifications = modification;
    }

    /** private create clone of this, only from within this class */
    private MessageDTO newCopy(BOP bop) {
        MessageDTO newMessageDTO = new MessageDTO(bop, CREATEMODE.CLONE);
        newMessageDTO.seq = this.seq;
        newMessageDTO.bops = this.bops; // only for demoing purposes
        newMessageDTO.message = this.message;
        newMessageDTO.modifications = this.modifications;
        return newMessageDTO;
    }

    // ==============
    // === public ===
    // ==============

    /** returns a cloned instance with BOP added, newMessage and appended modification */
    public MessageDTO transform(BOP bop, String newMessage, String modification) {
        MessageDTO newMessageDTO = newCopy(bop);
        newMessageDTO.bops.add(bop);
        newMessageDTO.message = newMessage;
        newMessageDTO.modifications += (newMessageDTO.modifications.length() > 0 ? " --> " : "") + modification;
        return newMessageDTO;
    }

    public String toStringBopIds() {
        return this.bop.toStringBopIds();
    }

    @Override
    public String toString() {
        return String.format("\nMessageDTO{Seq:%8d MSG:'%s' MODS:'%s'}\n%s",
                seq, message, modifications, 
                bops.stream().map(Object::toString).collect(Collectors.joining(",", "", "\n")));
    }
}
