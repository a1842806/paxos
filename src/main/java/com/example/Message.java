package com.example;

import java.io.Serializable;

/**
 * Represents messages exchanged between council members in the Paxos protocol.
 * Implements Serializable for network transmission.
 */
public class Message implements Serializable {
    /**
     * Defines the types of messages used in the Paxos protocol:
     * PREPARE - First phase proposal
     * PROMISE - Response to prepare
     * ACCEPT_REQUEST - Second phase proposal
     * ACCEPTED - Agreement to accept
     * NACK - Rejection response
     */
    public enum Type {
        PREPARE,
        PROMISE,
        ACCEPT_REQUEST, 
        ACCEPTED,
        NACK
    }

    // Message components
    private final Type type;
    private final int proposalNumber;
    private final String proposedValue;  // Candidate name
    private final int from;             // Sender ID
    
    public Message(Type type, int proposalNumber, String proposedValue, int from) {
        this.type = type;
        this.proposalNumber = proposalNumber;
        this.proposedValue = proposedValue;
        this.from = from;
    }

    // Getters
    public Type getType() { return type; }
    public int getProposalNumber() { return proposalNumber; }
    public String getProposedValue() { return proposedValue; }
    public int getFrom() { return from; }
}