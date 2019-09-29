package com.hoffi.minimal.microservices.microservice.common.dto;

import com.hoffi.minimal.microservices.microservice.helpers.SeqNr;

/** a DataTransferObject that may be modified by the same microservice or microservices downstream<br/>
 * on method <code>create(...)</code> id is counted up from 1 within each running process (ONLY by static <code>create(...)</code> method)<br/>
 * followup operations by the same microservice or downstream microservices should ONLY use the <code>transform(...)</code> method<br/>
 * which sets a new message, adds modification metadata, adds a BOP and keeps the id
 */
public class MessageDTO extends DTO {
    private static final long serialVersionUID = 1L;

    private enum CREATEMODE {
        NEW, CLONE
    };

    public static MessageDTO create(String message) {
        return new MessageDTO(message, "", CREATEMODE.NEW);
    }

    public static MessageDTO create(String message, String modifications) {
        return new MessageDTO(message, modifications, CREATEMODE.NEW);
    }

    public String seq = "-1";
    /** set to new content on any downstream microservice transformation on it */
    public String message = "<initial>";
    /** appended to on any downstream microservice transformation on it */
    public String modifications = "";


    /** private noArgs constructor (needed for jackson json deserialization */
    protected MessageDTO() {}

    /** private constructor */
    private MessageDTO(String message, String modification, CREATEMODE mode) {
        if (mode == CREATEMODE.NEW) {
            seq = SeqNr.nextSeqNr();
        }
        this.message = message;
        this.modifications = modification;
    }

    /** private create clone of this, only from within this class */
    private MessageDTO newCopy() {
        MessageDTO newMessageDTO = new MessageDTO(this.message, this.modifications, CREATEMODE.CLONE);
        newMessageDTO.seq = this.seq;
        return newMessageDTO;
    }

    // ==============
    // === public ===
    // ==============

    /** returns a cloned instance with BOP added, newMessage and appended modification */
    public MessageDTO transform(String newMessage, String modification) {
        MessageDTO newMessageDTO = newCopy();
        newMessageDTO.message = newMessage;
        newMessageDTO.modifications += (newMessageDTO.modifications.length() > 0 ? " --> " : "") + modification;
        return newMessageDTO;
    }


    @Override
    public String toString() {
        return String.format("\nMessageDTO{Seq:%8s MSG:'%s' MODS:'%s'}", seq, message, modifications);
    }
}
