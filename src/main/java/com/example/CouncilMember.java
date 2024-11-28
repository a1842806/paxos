package com.example;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Represents a member of the council implementing the Paxos consensus algorithm.
 * Each member can act as both proposer and acceptor in the consensus protocol.
 */
public class CouncilMember {
    private int id; // Unique identifier for this member
    private MemberBehavior behavior; // Network behavior simulation type for this member
    private Map<Integer, String> councilMembers; // Map of member IDs to their network addresses (host:port)
    private volatile boolean running = true; // Flag to control the running state of the member

    // Paxos protocol state variables
    private int proposalNumber = 0; // Proposal number counter for proposals initiated by this member
    private int promisedProposalNumber = -1; // Highest proposal number promised to so far
    private int acceptedProposalNumber = -1; // Highest proposal number accepted so far
    private String acceptedValue = null; // Value associated with the highest proposal accepted so far

    // Sets to track which members have promised or accepted proposals
    private Set<Integer> promisedMembers = new HashSet<>(); // Members that have sent PROMISE messages
    private Set<Integer> acceptedMembers = new HashSet<>(); // Members that have sent ACCEPTED messages

    private ServerSocket serverSocket; // Server socket to listen for incoming connections

    /**
     * Creates a new council member with specified behavior and network settings.
     *
     * @param id             Unique identifier for this member
     * @param name           Display name for the member
     * @param behavior       Network behavior simulation type
     * @param councilMembers Map of member IDs to network addresses
     * @param port           Network port to listen on
     * @throws IOException If the server socket fails to open
     */
    public CouncilMember(int id, String name, MemberBehavior behavior, Map<Integer, String> councilMembers, int port) throws IOException {
        this.id = id;
        this.behavior = behavior;
        this.councilMembers = councilMembers;
        this.serverSocket = new ServerSocket(port); // Initialize server socket to listen on the specified port
    }

    /**
     * Initiates an election by proposing a value to all other members.
     * Implements the first phase of the Paxos protocol (Prepare phase).
     *
     * @param proposedValue The value to propose for consensus
     */
    public void startElection(String proposedValue) {
        // Increment the proposal number for a new proposal
        proposalNumber++;
        System.out.println("[Member " + id + "] Starting election with proposal number " + proposalNumber + " and value: " + proposedValue);

        // Phase 1a: Send PREPARE messages to all other members
        System.out.println("[Member " + id + "] Sending PREPARE messages to all members");
        for (int memberId : councilMembers.keySet()) {
            if (memberId != id) { // Do not send to self
                // Create a PREPARE message with the current proposal number
                Message prepareMessage = new Message(
                        Message.Type.PREPARE,
                        proposalNumber,
                        null, // No value in PREPARE message
                        id
                );
                // Send the message to the member, applying any network behavior (delays, etc.)
                sendMessageWithBehavior(prepareMessage, memberId);
            }
        }

        // Wait for a majority of PROMISE messages from acceptors
        boolean hasPromiseMajority = waitForPromises();

        // Only proceed to Phase 2 if a majority of promises were received
        if (hasPromiseMajority) {
            // Phase 2a: Send ACCEPT_REQUEST messages to all members with the proposed value
            for (int memberId : councilMembers.keySet()) {
                if (memberId != id) {
                    // Create an ACCEPT_REQUEST message with the proposal number and proposed value
                    Message acceptRequest = new Message(
                            Message.Type.ACCEPT_REQUEST,
                            proposalNumber,
                            proposedValue,
                            id
                    );
                    // Send the accept request to the member
                    sendMessageWithBehavior(acceptRequest, memberId);
                }
            }

            // Wait for a majority of ACCEPTED messages from acceptors
            waitForAccepted();

            // If a majority of acceptances is received, declare success
            if (acceptedMembers.size() + 1 > councilMembers.size() / 2) {
                System.out.println("[Member " + id + "] Election successful! President elected: " + proposedValue);
                // Propagate the winning value to all members to ensure consistency
                propagateWinner(proposedValue, proposalNumber);
            } else {
                // Election failed due to insufficient acceptances
                System.out.println("[Member " + id + "] Election failed. No majority reached for " + proposedValue);
            }
        } else {
            // Election failed due to insufficient promises
            System.out.println("[Member " + id + "] Election failed. Did not receive majority of promises for " + proposedValue);
        }
    }

    /**
     * Waits for promise responses from other members.
     * Implements timeout mechanism to prevent infinite waiting.
     *
     * @return true if majority of promises received, false otherwise
     */
    private boolean waitForPromises() {
        System.out.println("[Member " + id + "] Waiting for PROMISE messages...");
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds timeout

        // Wait until a majority of promises is received or timeout occurs
        while (promisedMembers.size() + 1 <= councilMembers.size() / 2) {
            if (System.currentTimeMillis() - startTime > timeout) {
                // Timeout occurred
                System.out.println("[Member " + id + "] Promise phase timed out");
                return false;
            }
            try {
                Thread.sleep(100); // Sleep briefly to reduce CPU usage
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (promisedMembers.size() + 1 > councilMembers.size() / 2) {
            // Majority of promises received
            System.out.println("[Member " + id + "] Received majority of promises");
            return true;
        }
        return false;
    }

    /**
     * Waits for accept responses from other members after accept request phase.
     * Uses timeout to handle non-responsive members.
     */
    private void waitForAccepted() {
        System.out.println("[Member " + id + "] Waiting for ACCEPTED messages...");
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds timeout

        // Wait until a majority of acceptances is received or timeout occurs
        while (acceptedMembers.size() + 1 <= councilMembers.size() / 2) {
            if (System.currentTimeMillis() - startTime > timeout) {
                // Timeout occurred
                System.out.println("[Member " + id + "] Accept phase timed out");
                break;
            }
            try {
                Thread.sleep(100); // Sleep briefly to reduce CPU usage
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (acceptedMembers.size() + 1 > councilMembers.size() / 2) {
            // Majority of acceptances received
            System.out.println("[Member " + id + "] Received majority of accepts");
        }
    }

    /**
     * Routes incoming messages to appropriate handlers based on message type.
     * Central message processing method for the Paxos protocol.
     *
     * @param message The received message to process
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case PREPARE:
                handlePrepare(message);
                break;
            case PROMISE:
                handlePromise(message);
                break;
            case ACCEPT_REQUEST:
                handleAcceptRequest(message);
                break;
            case ACCEPTED:
                handleAccepted(message);
                break;
            case NACK:
                handleNack(message);
                break;
            default:
                break;
        }
    }

    /**
     * Handles PREPARE messages in the Paxos protocol.
     * Decides whether to promise based on the proposal number.
     *
     * @param message The PREPARE message to process
     */
    private void handlePrepare(Message message) {
        System.out.println("[Member " + id + "] Received PREPARE from Member " + message.getFrom() +
                " with proposal number " + message.getProposalNumber());
        // Check if the incoming proposal number is higher than any promised so far
        if (message.getProposalNumber() > promisedProposalNumber) {
            // Update promised proposal number and send a PROMISE message back
            System.out.println("[Member " + id + "] Promising to Member " + message.getFrom());
            promisedProposalNumber = message.getProposalNumber();
            // Send PROMISE message, including any previously accepted value
            Message promise = new Message(
                    Message.Type.PROMISE,
                    promisedProposalNumber,
                    acceptedValue, // Include accepted value, if any
                    id
            );
            sendMessageWithBehavior(promise, message.getFrom());
        } else {
            // Proposal number is not higher; reject by sending a NACK
            System.out.println("[Member " + id + "] Rejecting PREPARE from Member " + message.getFrom() +
                    " (already promised)");
            // Send NACK
            Message nack = new Message(
                    Message.Type.NACK,
                    promisedProposalNumber,
                    null,
                    id
            );
            sendMessageWithBehavior(nack, message.getFrom());
        }
    }

    /**
     * Processes PROMISE messages from other members.
     * Updates local state based on received promises.
     *
     * @param message The PROMISE message to process
     */
    private void handlePromise(Message message) {
        System.out.println("[Member " + id + "] Received PROMISE from Member " + message.getFrom());
        // Record that the member has sent a PROMISE
        promisedMembers.add(message.getFrom());
        System.out.println("[Member " + id + "] Current promises: " + promisedMembers.size() +
                "/" + councilMembers.size());
        // Update acceptedValue if the acceptor has previously accepted a value with a higher proposal number
        if (message.getProposalNumber() > acceptedProposalNumber && message.getProposedValue() != null) {
            // Update our accepted proposal number and value
            acceptedProposalNumber = message.getProposalNumber();
            acceptedValue = message.getProposedValue();
        }
    }

    /**
     * Handles ACCEPT_REQUEST messages in the protocol.
     * Decides whether to accept based on promised proposal number.
     *
     * @param message The ACCEPT_REQUEST message to process
     */
    private void handleAcceptRequest(Message message) {
        System.out.println("[Member " + id + "] Received ACCEPT_REQUEST from Member " + message.getFrom() +
                " for value: " + message.getProposedValue());

        // Check if the proposal number is at least as high as any promise we've made
        if (message.getProposalNumber() >= promisedProposalNumber) {
            // Accept the proposal
            System.out.println("[Member " + id + "] Accepting proposal from Member " + message.getFrom());
            acceptedProposalNumber = message.getProposalNumber();
            acceptedValue = message.getProposedValue();
            // Send ACCEPTED message back to the proposer
            Message accepted = new Message(
                    Message.Type.ACCEPTED,
                    acceptedProposalNumber,
                    acceptedValue,
                    id
            );
            sendMessageWithBehavior(accepted, message.getFrom());
        } else {
            // Proposal number is less than promised; reject
            System.out.println("[Member " + id + "] Rejecting ACCEPT_REQUEST from Member " + message.getFrom() +
                    " (promised to: " + promisedProposalNumber + ")");
            // Send NACK
            Message nack = new Message(
                    Message.Type.NACK,
                    promisedProposalNumber,
                    null,
                    id
            );
            sendMessageWithBehavior(nack, message.getFrom());
        }
    }

    /**
     * Processes ACCEPTED messages and checks for consensus.
     * Triggers winner propagation when consensus is reached.
     *
     * @param message The ACCEPTED message to process
     */
    private void handleAccepted(Message message) {
        System.out.println("[Member " + id + "] Received ACCEPTED from Member " + message.getFrom());
        // Record that this member has accepted the proposal
        acceptedMembers.add(message.getFrom());
        System.out.println("[Member " + id + "] Current accepts: " + acceptedMembers.size() +
                "/" + councilMembers.size());

        // Check if a majority of acceptors have accepted the proposal
        if (acceptedMembers.size() + 1 > councilMembers.size() / 2) {
            System.out.println("[Member " + id + "] Consensus reached! Propagating winner: " + message.getProposedValue());
            // Propagate the winning value to all members to ensure they learn the chosen value
            propagateWinner(message.getProposedValue(), message.getProposalNumber());
        }
    }

    /**
     * Propagates the winning value to all other members to ensure consensus.
     * This helps maintain consistency across the network even if some members
     * missed earlier messages.
     *
     * @param winningValue          The value that achieved consensus
     * @param winningProposalNumber The proposal number that won
     */
    private void propagateWinner(String winningValue, int winningProposalNumber) {
        // Only propagate if this is a new consensus that we haven't already accepted
        if (!winningValue.equals(acceptedValue)) {
            // Update our accepted value and proposal number
            acceptedValue = winningValue;
            acceptedProposalNumber = winningProposalNumber;

            // Send ACCEPT_REQUEST messages to all other members to inform them of the consensus
            for (int memberId : councilMembers.keySet()) {
                if (memberId != id) {
                    Message consensusMessage = new Message(
                            Message.Type.ACCEPT_REQUEST,
                            winningProposalNumber,
                            winningValue,
                            id
                    );
                    sendMessageWithBehavior(consensusMessage, memberId);
                }
            }
        }
    }

    /**
     * Handles negative acknowledgments (NACK) from other members.
     *
     * @param message The NACK message to process
     */
    private void handleNack(Message message) {
        System.out.println("[Member " + id + "] Received NACK from Member " + message.getFrom());
        // Additional handling for NACKs can be implemented here
    }

    /**
     * Implements behavior-based message sending with various delays.
     *
     * @param message Message to send
     * @param toId    Recipient member ID
     */
    private void sendMessageWithBehavior(Message message, int toId) {
        // Simulate network behavior based on the member's profile
        switch (behavior) {
            case IMMEDIATE_RESPONSE:
                // Send message immediately
                sendMessage(message, toId);
                break;
            case SMALL_DELAY:
                // Simulate a small delay before sending
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendMessage(message, toId);
                break;
            case LARGE_DELAY:
                // Simulate a large delay before sending
                try {
                    Thread.sleep(5000); // 5 seconds delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendMessage(message, toId);
                break;
            case NO_RESPONSE:
                // Simulate message loss or unresponsiveness by not sending the message
                break;
            default:
                // Default behavior is to send the message immediately
                sendMessage(message, toId);
                break;
        }
    }

    /**
     * Sends a message to another member over the network.
     *
     * @param message Message to send
     * @param toId    Recipient member ID
     */
    private void sendMessage(Message message, int toId) {
        System.out.println("[Member " + id + "] Sending " + message.getType() +
                " to Member " + toId);
        try {
            // Get the address of the recipient member
            String address = councilMembers.get(toId);
            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Establish a socket connection to the recipient
            Socket socket = new Socket(host, port);
            // Create an ObjectOutputStream to send the message object
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(message); // Send the message
            out.close();
            socket.close(); // Close the connection
        } catch (IOException e) {
            System.out.println("[Member " + id + "] Connection error: " + e.getMessage());
        }
    }

    /**
     * Starts listening for incoming messages on a separate thread.
     * Messages are processed according to the Paxos protocol.
     */
    public void listen() {
        // Start a new thread to listen for incoming connections
        new Thread(() -> {
            while (running) {
                try {
                    // Accept incoming connection
                    Socket clientSocket = serverSocket.accept();
                    // Create an ObjectInputStream to read the message object
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    // Read the message from the stream
                    Message message = (Message) in.readObject();
                    // Handle the received message according to the Paxos protocol
                    handleMessage(message);
                    in.close();
                    clientSocket.close();
                } catch (IOException | ClassNotFoundException e) {
                    if (!running) {
                        // If the member is shutting down, exit the loop
                        break;
                    } else {
                        System.out.println("[Member " + id + "] Connection error: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    // Add getters for testing and monitoring purposes
    public int getPromisedProposalNumber() {
        return promisedProposalNumber;
    }

    public int getAcceptedProposalNumber() {
        return acceptedProposalNumber;
    }

    public String getAcceptedValue() {
        return acceptedValue;
    }

    public Set<Integer> getPromisedMembers() {
        return new HashSet<>(promisedMembers);
    }

    public Set<Integer> getAcceptedMembers() {
        return new HashSet<>(acceptedMembers);
    }

    /**
     * Resets the member's state for a new election.
     * Clears all voting records and proposal numbers.
     */
    public void reset() {
        proposalNumber = 0;
        promisedProposalNumber = -1;
        acceptedProposalNumber = -1;
        acceptedValue = null;
        promisedMembers.clear();
        acceptedMembers.clear();
    }

    /**
     * Cleanly shuts down the member's network connections.
     */
    public void shutdown() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method for testing message handling.
     * Allows injecting messages directly for unit testing.
     *
     * @param message Message to process
     */
    public void testReceiveMessage(Message message) {
        handleMessage(message);
    }
}