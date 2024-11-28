package com.example;

/**
 * Defines possible behaviors for council members in the Paxos implementation.
 * These behaviors simulate different network and processing conditions.
 * - IMMEDIATE_RESPONSE: Member responds without delay
 * - SMALL_DELAY: Member responds with a small delay
 * - LARGE_DELAY: Member responds with a significant delay
 * - NO_RESPONSE: Member doesn't respond (simulates network failure)
 */
public enum MemberBehavior {
    IMMEDIATE_RESPONSE,
    SMALL_DELAY,
    LARGE_DELAY,
    NO_RESPONSE,
}