package com.example;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Paxos consensus implementation.
 * Tests various scenarios including simultaneous proposals,
 * immediate responses, and mixed behaviors.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaxosTest {
    private static final int BASE_PORT = 8000;
    private List<CouncilMember> members;
    private Map<Integer, String> councilMembers;

    @BeforeEach
    void setUp() {
        members = new ArrayList<>();
        councilMembers = new HashMap<>();
        
        // Initialize council members addresses
        for (int i = 1; i <= 9; i++) {
            councilMembers.put(i, "localhost:" + (BASE_PORT + i));
        }
    }

    @AfterEach
    void tearDown() {
        // Shutdown all members
        for (CouncilMember member : members) {
            member.shutdown();
        }
    }

    // 10 points - Simultaneous proposals
    @Test
    @Order(1)
    @DisplayName("Test 1: Simultaneous Proposals (10 points)")
    void testSimultaneousProposals() throws IOException, InterruptedException {
        System.err.println("\nTesting simultaneous proposals...\n");

        // Create members with immediate response
        createMemberWithBehavior(1, MemberBehavior.IMMEDIATE_RESPONSE);
        createMemberWithBehavior(2, MemberBehavior.LARGE_DELAY);
        createMembers(3, 9);

        // Start simultaneous proposals from two members
        Thread proposal1 = new Thread(() -> members.get(0).startElection("Member 1"));
        Thread proposal2 = new Thread(() -> members.get(1).startElection("Member 2"));

        proposal1.start();
        proposal2.start();

        // Wait for both proposals to complete
        proposal1.join();
        proposal2.join();
        
        // Allow time for consensus propagation
        Thread.sleep(2000);

        // Get the winning value
        String winningValue = members.get(0).getAcceptedValue();
        assertNotNull(winningValue, "A value should be chosen");

        // Verify ALL members (including both proposers) agreed on the same value
        for (CouncilMember member : members) {
            assertEquals(winningValue, member.getAcceptedValue(), 
                "All members including proposers should agree on winning value");
        }

        System.err.println("Final consensus: " + winningValue);
    }

    // 30 points - All immediate responses
    @Test
    @Order(2)
    @DisplayName("Test 2: All Immediate Responses (30 points)")
    void testAllImmediateResponses() throws IOException, InterruptedException {
        System.err.println("\nTesting all immediate responses...\n");

        // Create all members with immediate response
        for (int i = 1; i <= 9; i++) {
            createMemberWithBehavior(i, MemberBehavior.IMMEDIATE_RESPONSE);
        }

        // Start election with member 9
        members.get(8).startElection("Member 9");
        Thread.sleep(2000);

        // Start another election with different member
        members.get(4).startElection("Member 5");
        Thread.sleep(2000);

        // Verify that all members converged to the same value
        String finalValue = members.get(0).getAcceptedValue();
        assertNotNull(finalValue, "A value should be chosen");
        
        for (CouncilMember member : members) {
            assertEquals(finalValue, member.getAcceptedValue(), 
                "All members should converge to the same value after multiple proposals");
        }

        System.err.println("Final consensus: " + finalValue);
    }

    // 30 points - Mixed behaviors and offline members
    @Test
    @Order(3)
    @DisplayName("Test 2: Mixed Behaviors and Offline Members")
    void testMixedBehaviorsAndOfflineMembers() throws IOException, InterruptedException {
        System.err.println("\nTesting mixed behaviors and offline members...\n");

        // Create members with different behaviors
        createMemberWithBehavior(1, MemberBehavior.IMMEDIATE_RESPONSE);
        createMemberWithBehavior(2, MemberBehavior.SMALL_DELAY);
        createMemberWithBehavior(3, MemberBehavior.LARGE_DELAY);
        createMembers(4, 9);

        // Start election with member 2
        members.get(1).startElection("Member 2");
        Thread.sleep(2000);

        // Verify initial consensus
        String firstConsensus = members.get(1).getAcceptedValue();
        System.err.println("Initial consensus: " + firstConsensus);
        
        // Simulate member 2 going offline
        members.get(1).shutdown();

        // Start new election with member 3
        members.get(2).startElection("Member 3");
        Thread.sleep(7000); // Longer wait due to delays

        // Get final consensus value
        String finalValue = null;
        for (CouncilMember member : members) {
            if (member.getAcceptedValue() != null) {
                finalValue = member.getAcceptedValue();
                break;
            }
        }

        assertNotNull(finalValue, "A consensus value should exist");

        // Check that all active members eventually agree on the same value
        int consensusCount = 0;
        for (int i = 0; i < members.size(); i++) {
            if (i != 1) { // Skip offline member
                CouncilMember member = members.get(i);
                if (finalValue.equals(member.getAcceptedValue())) {
                    consensusCount++;
                }
            }
        }

        System.err.println("Final consensus: " + finalValue);

        // Verify majority consensus was reached
        assertTrue(consensusCount > (members.size() - 1) / 2, 
            "Majority consensus should be reached despite mixed behaviors");
    }

    private void createMembers(int start, int end) throws IOException {
        for (int i = start; i <= end; i++) {
            // Randomly assign behavior to each member, add a modifier that NO_RESPONSE is less likely
            MemberBehavior behavior = MemberBehavior.values()[(int) (Math.random() * MemberBehavior.values().length)];
            if (behavior == MemberBehavior.NO_RESPONSE && Math.random() < 0.5) {
                behavior = MemberBehavior.values()[(int) (Math.random() * MemberBehavior.values().length)];
            }
            System.err.println("Member " + i + " behavior: " + behavior);
            createMemberWithBehavior(i, behavior);
        }
    }

    private void createMemberWithBehavior(int id, MemberBehavior behavior) throws IOException {
        CouncilMember member = new CouncilMember(
            id,
            "Member" + id,
            behavior,
            councilMembers,
            BASE_PORT + id
        );
        member.listen();
        members.add(member);
    }
}
