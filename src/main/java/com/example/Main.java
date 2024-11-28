package com.example;

import java.io.IOException;
import java.util.*;

/**
 * Main class that demonstrates the Paxos consensus algorithm
 * by running multiple scenarios with different council members.
 */
public class Main {
    /**
     * Main entry point that sets up and runs multiple election scenarios
     * to demonstrate the Paxos consensus algorithm in action.
     */
    public static void main(String[] args) {
        // Initialize council members with IDs and their respective addresses
        Map<Integer, String> councilMembers = new HashMap<>();
        int basePort = 5000;
        for (int i = 1; i <= 9; i++) {
            councilMembers.put(i, "localhost:" + (basePort + i));
        }

        // Create and start CouncilMember instances
        List<CouncilMember> members = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            try {
                // Randomly assign member behavior
                MemberBehavior behavior = MemberBehavior.values()[(int) (Math.random() * MemberBehavior.values().length)];
                if (behavior == MemberBehavior.NO_RESPONSE && Math.random() < 0.5) {
                    behavior = MemberBehavior.values()[(int) (Math.random() * MemberBehavior.values().length)];
                }
                if (i == 1) {
                    behavior = MemberBehavior.IMMEDIATE_RESPONSE;
                } else if (i == 2) {
                    behavior = MemberBehavior.LARGE_DELAY;
                } else if (i == 3) {
                    behavior = MemberBehavior.SMALL_DELAY;
                }

                CouncilMember member = new CouncilMember(
                    i,
                    "Member" + i,
                    behavior,
                    councilMembers,
                    basePort + i
                );
                member.listen();
                members.add(member);

                System.out.println("Member " + i + " has behavior: " + behavior);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Allow some time for all members to start listening
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Scenario 1: Member 1 proposes
        System.out.println("\n=== Scenario 1: Member 1 proposes ===\n");
        members.get(0).startElection("Member 1");
        // Wait for the election to complete
        try { Thread.sleep(6000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Reset members' state for the next scenario
        for (CouncilMember member : members) {
            member.reset();
        }

        // Scenario 2: Member 2 proposes
        System.out.println("\n=== Scenario 2: Member 2 proposes ===\n");
        members.get(1).startElection("Member 2");
        // Wait for the election to complete
        try { Thread.sleep(6000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Reset members' state for the next scenario
        for (CouncilMember member : members) {
            member.reset();
        }

        // Scenario 3: Member 3 proposes
        System.out.println("\n=== Scenario 3: Member 3 proposes ===\n");
        members.get(2).startElection("Member 3");
        // Wait for the election to complete
        try { Thread.sleep(6000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Shutdown all members
        for (CouncilMember member : members) {
            member.shutdown();
        }
    }
}


