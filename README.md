# Paxos Consensus Implementation

A Java implementation of the Paxos consensus algorithm for distributed systems. This implementation simulates network behaviors and allows testing different consensus scenarios.

## Prerequisites

- Java 22 or higher
- Maven 3.x

## Project Structure

```txt
paxos/
├── src/
│   ├── main/java/com/example/
│   │   ├── CouncilMember.java    # Core Paxos implementation
│   │   ├── Message.java          # Message protocol
│   │   ├── MemberBehavior.java   # Network behaviors
│   │   └── Main.java             # Demo scenarios
│   └── test/java/com/example/
│       └── PaxosTest.java        # Test scenarios
└── [pom.xml]
```

## Building the Project

To build the project, run:

```bash
# Clone the repository
git clone https://github.com/a1842806/paxos

# Navigate to project directory
cd paxos

# Build the project
mvn clean install
```

## Running Test

```bash
mvn test
```

### Test Scearios

1️⃣ Simultaneous Proposals (10 points)
Two concurrent proposals

2️⃣ Immediate Responses (30 points)
All members respond instantly

3️⃣ Mixed Behaviors & Offline Members (30 points)
Various network conditions

### Demo Execution

```bash
mvn exec:java
```

## Network Behaviors
| Behavior | Description | Delay |
|----------|-------------|-------|
| IMMEDIATE_RESPONSE | Instant reply | 0ms |
| SMALL_DELAY | Minor latency | 1000ms |
| LARGE_DELAY | Network congestion | 6000ms |
| NO_RESPONSE | Message loss | ∞ |