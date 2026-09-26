# Distributed Systems Interview Questions

Core interview questions covering distributed systems theory, consistency models, replication quorums, clocks, consensus, and distributed locking.

<!-- --8<-- [start:basic] -->
## Basic

### What are the eight fallacies of distributed computing, and why do they cause production outages?

??? question "Reveal answer"
    **Short Answer:** The eight fallacies are false assumptions engineers make when designing network systems: (1) The network is reliable, (2) Latency is zero, (3) Bandwidth is infinite, (4) The network is secure, (5) Topology doesn't change, (6) There is one administrator, (7) Transport cost is zero, (8) The network is homogeneous. Ignoring them leads to missing timeouts, cascading connection pool exhaustion, unencrypted internal traffic, and availability collapse during network reconfigurations.

    ??? example "Example"
        ```yaml
        # Antidote to Fallacy #1 & #2: Explicit timeouts on every network client
        resilience4j:
          timelimiter:
            instances:
              paymentService:
                timeoutDuration: 2000ms
                cancelRunningFuture: true
        ```

### What does the CAP theorem state, and why is "CA" impossible during a network partition?

??? question "Reveal answer"
    **Short Answer:** In any asynchronous network subject to partitions ($P$), a distributed data store can guarantee either Consistency ($C$ — linearizability, every read receives the latest write) or Availability ($A$ — every non-failing node returns a successful response), but never both. A system cannot choose "CA" because network partitions are an unavoidable physical reality of cables, switches, and clouds; when a partition occurs, the system must either reject writes on the minority side (favoring $C$) or accept writes on both sides risking data divergence (favoring $A$).

    ??? example "Example"
        ```mermaid
        flowchart LR
            subgraph DC1["Data Center 1 (Majority)"]
                N1["Node 1 (Leader)"]
                N2["Node 2 (Follower)"]
            end
            subgraph DC2["Data Center 2 (Minority)"]
                N3["Node 3 (Follower)"]
            end
            DC1 -.-x|Network Partition| DC2
            Note over N3: In a CP system: N3 rejects writes!<br/>In an AP system: N3 accepts writes (Stale/Divergent)!
        ```

### What is the PACELC theorem, and how does it expand upon CAP during normal operation?

??? question "Reveal answer"
    **Short Answer:** The PACELC theorem states: If there is a Partition ($P$), a system trades Availability ($A$) versus Consistency ($C$); Else ($E$, normal operation without partitions), the system trades Latency ($L$) versus Consistency ($C$). CAP only describes behavior during rare partition events; PACELC explains why systems like Cassandra/DynamoDB choose low latency over strong consistency every millisecond of every day under normal conditions (PA/EL), whereas systems like Spanner or CockroachDB choose strict consistency at the cost of higher commit latency (PC/EC).

    ??? example "Example"
        ```text
        PACELC Classifications:
        - Apache Cassandra:   PA / EL  (Partition -> Available, Else -> Low Latency)
        - Google Spanner:     PC / EC  (Partition -> Consistent, Else -> Consistent)
        - MongoDB (w:majority): PC / EC (Partition -> Consistent, Else -> Consistent)
        ```

### What is partial failure, and how does it fundamentally distinguish distributed systems from single-node systems?

??? question "Reveal answer"
    **Short Answer:** In a single-node system, if the CPU or memory fails, the entire application crashes deterministically. In a distributed system, **partial failure** means some network links, switches, or nodes fail while others continue operating normally. A caller sending a network request cannot distinguish whether the remote node crashed before processing, the request timed out on the wire, or the response packet was dropped on the return path, making distributed failure inherently non-deterministic.

    ??? example "Example"
        ```mermaid
        sequenceDiagram
            participant Client
            participant Server
            Client->>Server: POST /charge-card ($100)
            Note over Server: Server charges card successfully!
            Server--xClient: TCP ACK dropped by network switch!
            Note over Client: Client experiences TimeoutException.<br/>Did the payment succeed or fail? (Unknown!)
        ```

### What are the trade-offs between synchronous RPC and asynchronous messaging?

??? question "Reveal answer"
    **Short Answer:** Synchronous RPC (REST, gRPC) provides immediate request-response semantics and simpler programming models, but introduces tight temporal and availability coupling; if the downstream service is down or slow, the caller blocks and may exhaust thread/connection pools. Asynchronous messaging (Kafka, RabbitMQ, SQS) decouples producer and consumer lifecycles, provides natural buffering and rate smoothing (backpressure), but introduces eventual consistency, out-of-band error handling, and complex distributed debugging.

    ??? example "Example"
        ```text
        Availability Comparison:
        Synchronous Chain (A -> B -> C):  Availability = 0.999 * 0.999 * 0.999 = 99.7%
        Asynchronous Decoupled (A -> Queue -> B): Service A availability = 99.9% (Independent of B)
        ```

### What is an idempotency key, and how does it provide at-least-once to effectively-once processing?

??? question "Reveal answer"
    **Short Answer:** An idempotency key is a unique client-generated token (typically a UUID) attached to mutating requests. The receiving service stores the key in an atomic deduplication store (e.g. Redis `SET NX` or SQL unique constraint). If a duplicate request arrives (due to network timeout or retry), the service detects the existing key, bypasses mutation execution, and returns the cached response, safely converting at-least-once network delivery into effectively-once business processing.

    ??? example "Example"
        ```sql
        -- Database atomic idempotency table
        CREATE TABLE processed_requests (
            idempotency_key VARCHAR(64) PRIMARY KEY,
            resource_id VARCHAR(64) NOT NULL,
            response_payload JSONB NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );
        ```

### What is Read-Your-Writes consistency, and how can it be implemented on an eventually consistent database?

??? question "Reveal answer"
    **Short Answer:** Read-Your-Writes (monotonic read-after-write) consistency guarantees that a user who updates a record will always observe their own update on subsequent reads, even if other users observe stale data temporarily. It can be implemented by: (1) routing reads to the primary/master node for $N$ seconds after a user performs a write, (2) recording the write version/LSN in a client session cookie and requiring replica reads to wait until the replica has replayed up to that LSN, or (3) reading with quorum consistency ($R + W > N$).

    ??? example "Example"
        ```java
        // Client-side session routing for Read-Your-Writes
        if (session.getLastWriteTimestamp() != null && 
            Duration.between(session.getLastWriteTimestamp(), Instant.now()).toSeconds() < 5) {
            // Route read to Primary DB to guarantee read-your-writes
            return primaryDataSource.query(sql);
        } else {
            // Safe to read from asynchronous Read Replica
            return replicaDataSource.query(sql);
        }
        ```

### How does the quorum formula R + W > N guarantee strong consistency across replicas?

??? question "Reveal answer"
    **Short Answer:** In an $N$-node cluster, requiring $W$ nodes to acknowledge writes and querying $R$ nodes on reads guarantees that if $R + W > N$, the read set and write set must overlap by at least one node by the Pigeonhole Principle. The overlapping node is guaranteed to hold the latest version of the record. The client compares version numbers or vector clocks across the $R$ responses and returns the newest version, preventing stale reads.

    ??? example "Example"
        ```text
        Cluster Configuration: N = 5 replicas
        Write Quorum: W = 3 (Nodes {1, 2, 3})
        Read Quorum:  R = 3 (Nodes {3, 4, 5})
        Overlap: {1, 2, 3} ∩ {3, 4, 5} = {Node 3} -> Node 3 returns the latest write!
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### What is the difference between Linearizability and Serializability?

??? question "Reveal answer"
    **Short Answer:** Linearizability is a **single-operation, real-time guarantee**: every read must return the most recent write in global wall-clock time, behaving as if a single central copy of the object exists. Serializability is a **multi-operation, non-real-time transactional guarantee**: transactions execute concurrently such that the result is equivalent to *some* serial execution order, but imposes no real-time order constraint. A system can be Serializable without being Linearizable, and vice versa. The combination of both is **Strict Serializability** (External Consistency).

    ??? example "Example"
        ```text
        - Linearizability: Applies to single register / object. Respects global physical time order.
        - Serializability: Applies to multi-statement transactions. Guarantees isolation, not freshness.
        - Strict Serializability: Multi-statement transactions executed in global real-time order (Google Spanner).
        ```

### Why is physical wall-clock synchronization (NTP) unreliable for ordering distributed transactions?

??? question "Reveal answer"
    **Short Answer:** Physical quartz clocks drift by milliseconds per day due to heat and voltage variations. NTP synchronizes clocks over asynchronous networks subject to variable packet delays, leaving uncertainty bounds of 5–50+ milliseconds across data centers. Furthermore, NTP can step clocks backward, and leap seconds introduce non-monotonic time. Using `System.currentTimeMillis()` for Last-Write-Wins (LWW) causes transactions occurring later in real time to be assigned smaller timestamps and silently discarded if the executing node's clock is running behind.

    ??? example "Example"
        ```text
        Node A (Clock: 12:00:00.050) -> Writes Order#1 (assigned t=50ms)
        Node B (Clock running slow: 12:00:00.010) -> Writes Order#2 at t=60ms real time (assigned t=10ms)
        LWW Conflict Resolution: Compares 50ms vs 10ms -> Order#2 is SILENTLY DISCARDED!
        ```

### How do Vector Clocks detect concurrent conflicting writes compared to Lamport Timestamps?

??? question "Reveal answer"
    **Short Answer:** A Lamport timestamp is a single integer counter that provides a strict total order ($A \to B \implies L(A) < L(B)$), but cannot determine whether two events were causally related or concurrent ($L(A) < L(B)$ does not imply $A \to B$). A Vector Clock is an array of counters containing an entry for every node. By comparing vector components, a system can mathematically distinguish whether event $A$ causally preceded event $B$ ($V_A < V_B$) or whether $A$ and $B$ occurred concurrently ($V_A \parallel V_B$), triggering application-level conflict resolution.

    ??? example "Example"
        ```text
        Node 1: V_A = [2, 0]  (Node 1 executed 2 events)
        Node 2: V_B = [0, 1]  (Node 2 executed 1 event independently)
        Comparison: 
        V_A[0] > V_B[0] (2 > 0), but V_A[1] < V_B[1] (0 < 1)
        Result: V_A || V_B -> CONCURRENT WRITE CONFLICT DETECTED!
        ```

### Why does a Redis SET NX PX distributed lock fail without fencing tokens under client GC pauses?

??? question "Reveal answer"
    **Short Answer:** If Client 1 acquires a lock with a 10-second TTL and immediately enters an 11-second Stop-The-World JVM GC pause or network stall, Redis automatically expires the lease and grants the lock to Client 2. Client 2 writes to shared storage. Client 1 wakes up from its GC pause unaware that its lease expired and executes its write, corrupting Client 2's data. A fencing token (a monotonically increasing counter returned on lock acquisition and verified by storage) prevents this because storage rejects Client 1's write with an outdated token.

    ??? example "Example"
        ```sql
        -- Fencing token verification at storage layer
        UPDATE storage_manifest
        SET file_uri = :newUri,
            last_fencing_token = :fencingToken
        WHERE partition_id = :partitionId
          AND last_fencing_token < :fencingToken; -- Rejects stale lock holders!
        ```

### What is Split-Brain syndrome, and how do quorum-based consensus protocols prevent it?

??? question "Reveal answer"
    **Short Answer:** Split-Brain occurs when a network partition divides a cluster into isolated sub-networks, and nodes on both sides believe the other side is dead. If both sides elect a leader and accept writes, data diverges irreversibly. Quorum-based consensus protocols (Raft, Paxos) prevent split-brain by mandating that any leader election or write operation require confirmation from a strict majority ($\lfloor N/2 \rfloor + 1$). In an odd-sized cluster (e.g. 5 nodes), only one partition can ever assemble 3 nodes; the minority partition cannot elect a leader and ceases accepting writes.

    ??? example "Example"
        ```text
        Cluster Size: N = 5. Strict Majority: floor(5/2) + 1 = 3 nodes.
        Partition: Group A has 3 nodes; Group B has 2 nodes.
        - Group A: Assembles 3/5 votes -> Elects leader -> Accepts writes.
        - Group B: Assembles 2/5 votes (< 3) -> Cannot elect leader -> Rejects writes.
        ```

### How does Consistent Hashing with virtual nodes minimize cache rehashing during cluster resizing?

??? question "Reveal answer"
    **Short Answer:** Standard modulo hashing ($\text{hash}(key) \pmod N$) remaps almost every key ($k \times (N-1)/N$) when adding or removing a node, causing massive cache stampedes and database overloads. Consistent Hashing maps both node IDs and keys onto a circular hash ring ($0 \text{ to } 2^{32}-1$). A key is stored on the first node clockwise from its hash position. When a node is added or removed, only $K/N$ keys on average are remapped. Virtual nodes (assigning 100–300 hash points per physical server) ensure uniform distribution and prevent hot spots.

    ??? example "Example"
        ```mermaid
        flowchart TD
            subgraph Ring["Consistent Hash Ring (0 to 2^32 - 1)"]
                N1["Node A (Virtual 1)"] --> K1["Key 101"]
                K1 --> N2["Node B (Virtual 1)"]
                N2 --> K2["Key 202"]
                K2 --> N3["Node C (Virtual 1)"]
            end
        ```

### How does a Gossip Protocol achieve decentralized cluster membership and failure detection?

??? question "Reveal answer"
    **Short Answer:** A Gossip Protocol (epidemic protocol) is a decentralized communication pattern where every node periodically selects $k$ random peer nodes and exchanges membership lists and heartbeat counters. Over $O(\log N)$ gossip rounds, state updates propagate across the entire cluster without requiring a central coordinator. Failure detection (e.g. $\phi$-Accrual Failure Detector) measures heartbeat intervals statistically, generating a suspicion level rather than a binary up/down state to handle temporary network jitter gracefully.

    ??? example "Example"
        ```text
        Gossip Round t=0: Node 1 discovers Node 4 is down.
        t=1: Node 1 gossips to Node 2 and Node 3.
        t=2: Nodes 1, 2, 3 gossip to random peers.
        Within O(log N) rounds, all 1,000 nodes converge on the cluster topology.
        ```

### What is the difference between Rate Limiting and Load Shedding in distributed services?

??? question "Reveal answer"
    **Short Answer:** **Rate Limiting** is an external protection mechanism enforced at the perimeter (API Gateway) based on client identity (e.g. 100 req/sec per API key), rejecting callers who exceed their allocated quota regardless of whether the server is busy. **Load Shedding** is an internal survival mechanism enforced by the service itself based on internal health signals (CPU, thread pool queue depth, P99 latency); when the system detects imminent saturation, it rejects low-priority incoming requests immediately with HTTP 503 to ensure active in-flight transactions complete successfully.

    ??? example "Example"
        ```java
        // Load shedding filter in Java Spring Boot
        if (threadPoolExecutor.getQueue().size() > 500 || 
            osBean.getCpuLoad() > 0.85) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.getWriter().write("Server overloaded: load shed");
            return;
        }
        ```

---

### What is the Gossip Protocol, and how does it achieve decentralized cluster membership and failure detection?

??? question "Reveal answer"
    **Short Answer:** The **Gossip Protocol** (epidemic algorithm) is a decentralized communication protocol where nodes periodically exchange state and membership information with randomly selected peers, propagating updates exponentially across the cluster ($O(\log N)$ rounds) without requiring a central coordinator or shared registry.

    **Key Mechanisms:**
    - **Failure Detection (SWIM Protocol):** Node A sends an indirect ping to Node B via Node C. If Node B does not respond, it is marked `SUSPECT`. If it remains unresponsive for a grace period, it is declared `DEAD` and purged.
    - **Anti-Entropy vs Rumor Mongering:** Rumor mongering floods updates quickly across nodes; anti-entropy periodically reconciles complete datasets between pairs of nodes to resolve missed messages.
    - **Applications:** Used in Apache Cassandra, Amazon Dynamo, Consul, and HashiCorp Serf for cluster health and node membership.

    ??? example "Example"
        ```text
        Gossip Round Progression:
        Round 1: Node 1 gossips to Node 4
        Round 2: Nodes 1 & 4 gossip to Nodes 2 & 5 (4 nodes infected)
        Round 3: All 4 nodes gossip to remaining nodes (Cluster converges in O(log N) time)
        ```

---

### What is Consistent Hashing with virtual nodes (vnodes), and how does it prevent hot spots during cluster rebalancing?

??? question "Reveal answer"
    **Short Answer:** In traditional modulo hashing (`hash(key) % N`), adding or removing a single node forces almost all keys to relocate ($N/(N+1)$ fraction reshuffled). **Consistent Hashing** maps both node identifiers and data keys onto a continuous $2^{32}-1$ integer ring. Keys are assigned to the first node encountered clockwise. When a node joins or leaves, only $1/N$ of keys are moved to/from adjacent neighbors.

    **Role of Virtual Nodes (Vnodes):**
    - **Non-Uniform Distribution Problem:** With few physical nodes, random ring placement creates large ring gaps, overloading specific nodes.
    - **Vnode Solution:** Each physical server is assigned multiple virtual tokens (e.g. 128 or 256 vnodes) spread across the ring. This balances data uniformly across physical servers and allows rebalancing load to be shared across all surviving nodes simultaneously instead of dumping 100% of the lost node's load onto a single immediate neighbor.

    ??? example "Example"
        ```mermaid
        flowchart TD
            Ring(("Consistent Hash Ring 0 to 2^32-1"))
            NodeA1["Node A (Vnode 1)"]
            NodeB1["Node B (Vnode 1)"]
            NodeA2["Node A (Vnode 2)"]
            NodeB2["Node B (Vnode 2)"]
            Key1["Key: user:1001"] --> NodeB1
            Key2["Key: user:1002"] --> NodeA2
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### How do you design a distributed locking service that remains strictly safe against arbitrary JVM Stop-The-World GC pauses?

??? question "Reveal answer"
    **Short Answer:** Never rely on the client's internal perception of lock validity.
    1. **Coordination Store**: Use a strongly consistent consensus engine (etcd, ZooKeeper, or Redis) where lock acquisition returns a monotonically increasing integer token (fencing token).
    2. **Watchdog Heartbeat**: Run a background thread renewing lease TTL while the client executes.
    3. **Enforcement at Storage Gate**: The shared storage service (database or S3 manifest) must reject any write whose fencing token is less than or equal to the highest token previously committed. When a paused client wakes up and attempts to write with an outdated token, the storage layer blocks the write with a concurrency violation.

    ??? example "Example"
        ```mermaid
        sequenceDiagram
            participant C1 as Client 1 (Paused)
            participant Lock as Lock Service
            participant Storage as DB Storage Gate

            C1->>Lock: AcquireLock()
            Lock-->>C1: Granted (Token = 101)
            Note over C1: STW GC Pause (15 seconds)
            Note over Lock: Lease expires -> Grants to Client 2 (Token = 102)
            Note over Storage: Client 2 writes with Token = 102 (Committed)
            Note over C1: Client 1 wakes up!
            C1->>Storage: Write data (Token = 101)
            Storage--xC1: REJECTED! (101 <= 102)
        ```

### How does a Hybrid Logical Clock (HLC) bound time drift while providing monotonic causal ordering?

??? question "Reveal answer"
    **Short Answer:** An HLC timestamp is a pair $\langle l, c \rangle$ where $l$ represents physical time and $c$ is a logical counter. On local events, $l = \max(l, \text{physical\_time})$; if physical time hasn't changed, $c$ increments. On receiving a message with timestamp $\langle l_{\text{msg}}, c_{\text{msg}} \rangle$, the node sets $l = \max(l, l_{\text{msg}}, \text{physical\_time})$, resetting or incrementing $c$ accordingly. HLC guarantees: (1) if $e_1 \to e_2$, then $HLC(e_1) < HLC(e_2)$, and (2) $|l - \text{physical\_time}| \le \epsilon$ (bounded drift from physical wall-clock time).

    ??? example "Example"
        ```java
        // HLC State Update on Message Receive
        long physicalNow = System.currentTimeMillis();
        long lNew = Math.max(Math.max(this.l, msg.l), physicalNow);
        long cNew;
        if (lNew == this.l && lNew == msg.l) {
            cNew = Math.max(this.c, msg.c) + 1;
        } else if (lNew == this.l) {
            cNew = this.c + 1;
        } else if (lNew == msg.l) {
            cNew = msg.c + 1;
        } else {
            cNew = 0;
        }
        this.l = lNew;
        this.c = cNew;
        ```

### How do Conflict-Free Replicated Data Types (CRDTs) achieve deterministic multi-master replication without centralized locks?

??? question "Reveal answer"
    **Short Answer:** CRDTs are mathematical data structures whose state update operations form a bounded semilattice with a join operator ($\sqcup$) that is **Associative**, **Commutative**, and **Idempotent** (ACI). Because updates can be applied in any order, duplicated without penalty, and merged deterministically:
    - **Pn-Counter (Positive-Negative)**: Tracks separate increment and decrement vectors per node.
    - **LWW-Element-Set**: Resolves set additions and removals via timestamps.
    - **Observed-Removed Set (OR-Set)**: Assigns unique tags to additions, allowing deletions to remove only observed tags without coordination locks.

    ??? example "Example"
        ```text
        State-based CRDT Merge: State_Merged = State_A ⊔ State_B
        - Associative:   (A ⊔ B) ⊔ C = A ⊔ (B ⊔ C)
        - Commutative:   A ⊔ B = B ⊔ A (Message arrival order does not matter!)
        - Idempotent:    A ⊔ A = A     (Duplicate network deliveries have zero effect!)
        ```

### Compare the consistency and availability trade-offs of Dynamo-style vs Spanner-style architectures under PACELC.

??? question "Reveal answer"
    **Short Answer:**
    - **Dynamo-style (Cassandra, Amazon DynamoDB)**: Prioritizes Availability and Latency (**PA/EL**). Uses leaderless replication, sloppy quorums with hinted handoff, and asynchronous anti-entropy. Writes succeed with local low latency even during cross-region partitions, but reads risk returning stale or conflicting versions (divergence resolved via CRDTs or LWW).
    - **Spanner-style (Google Spanner, CockroachDB)**: Prioritizes Consistency and Correctness (**PC/EC**). Uses multi-Paxos/Raft consensus groups per range, hardware TrueTime with atomic clocks, and two-phase locking. Guarantees global linearizability and strict serializability, but network partitions block writes in minority regions and commit latency includes consensus roundtrips and clock uncertainty wait intervals ($2\epsilon$).

    ??? example "Example"
        ```text
        Feature                   Dynamo-Style (Cassandra)      Spanner-Style (CockroachDB)
        ------------------------------------------------------------------------------------
        PACELC                    PA / EL                       PC / EC
        Consensus Mechanism       Quorum Overlap (R+W>N)        Raft / Multi-Paxos
        Transaction Isolation     Row-level / Eventual          Strict Serializability
        Write Latency             Sub-5ms (Local disk/memory)   15-50ms (Consensus + TrueTime)
        Cross-Region Partition    Accepts writes (Diverges)     Rejects writes on minority
        ```

### Why is Two-Phase Commit considered an anti-pattern across microservices, and how does the Saga pattern solve it?

??? question "Reveal answer"
    **Short Answer:** 2PC requires all participant databases to acquire row locks during Phase 1 (Prepare) and hold them until Phase 2 (Commit) arrives over the network. If the coordinator crashes in Phase 2, participants remain blocked in an In-Doubt state holding locks indefinitely. Furthermore, 2PC couples availability ($A_{\text{total}} = \prod A_i$), turning one slow service into a system-wide bottleneck. The **Saga pattern** replaces distributed locking with a sequence of local database transactions: each service commits locally in milliseconds, and if a subsequent step fails, compensating transactions are triggered asynchronously to reverse previous changes.

    ??? example "Example"
        ```mermaid
        flowchart LR
            T1["1. Create Pending Order<br/>(Order Service)"] --> T2["2. Reserve Inventory<br/>(Inventory Service)"]
            T2 --> T3["3. Charge Payment<br/>(Payment Service - FAILS!)"]
            T3 -.->|Compensate| C2["C2: Release Inventory<br/>(Compensating Action)"]
            C2 -.->|Compensate| C1["C1: Cancel Order<br/>(Compensating Action)"]
        ```

---

### What is the Byzantine Fault Tolerance (BFT) problem, and how does it differ from Crash-Fault Tolerance (CFT)?

??? question "Reveal answer"
    **Short Answer:** **Crash-Fault Tolerance (CFT)** assumes nodes either operate correctly or crash silently (stop-fail model), where nodes never lie or forge messages. **Byzantine Fault Tolerance (BFT)** assumes nodes can fail arbitrarily: they may crash, lie, forge signatures, delay packets, or send contradictory messages to different peers (malicious or corrupted state).

    **Quorum Comparison:**
    - **CFT (Raft, Paxos):** Requires $2f + 1$ total nodes to tolerate $f$ crashed nodes (e.g. 3 nodes tolerate 1 crash; 5 nodes tolerate 2 crashes).
    - **BFT (PBFT, Tendermint):** Requires $3f + 1$ total nodes to tolerate $f$ Byzantine nodes (e.g. 4 nodes tolerate 1 traitor; 7 nodes tolerate 2 traitors) because the honest majority must out-vote both the faulty nodes and their deceptive messages.

    ??? example "Example"
        ```text
        Consensus Quorum Comparison:
        Fault Type            Algorithm Examples        Minimum Nodes for f Failures
        Crash-Fault (CFT)     Raft, Paxos, ZooKeeper    2f + 1 (5 nodes tolerate 2 crashes)
        Byzantine (BFT)       PBFT, Tendermint, Raft-BFT 3f + 1 (7 nodes tolerate 2 traitors)
        ```

---

### How do Hybrid Logical Clocks (HLC) combine physical NTP time with Lamport logical clocks to order distributed transactions?

??? question "Reveal answer"
    **Short Answer:** Pure physical clocks (NTP) suffer from non-deterministic clock skew and backwards jumps, causing causality inversions. Pure Lamport logical clocks preserve causal ordering ($A \to B \implies L(A) < L(B)$) but have zero relation to wall-clock physical time, making time-range queries impossible. **Hybrid Logical Clocks (HLC)** track a composite coordinate $(l.e, c.e)$: a physical component $l$ bounded by physical NTP time, and a logical increment counter $c$ that advances when physical clock drift stalls or jumps backwards.

    **Properties:**
    - **Causal Consistency:** If event $e$ caused $e'$, then $HLC(e) < HLC(e')$.
    - **Bounded Physical Drift:** $|l.e - \text{pt}.e| \le \epsilon$, where $\epsilon$ is maximum NTP clock error.
    - **Adoption:** Used in modern distributed SQL databases like CockroachDB, YugabyteDB, and MongoDB for distributed serializable transactions without requiring atomic clocks.

    ??? example "Example"
        ```text
        HLC Coordinate Update on Message Receive (m):
        l' = max(l_local, pt_local, m.l)
        if l' == l_local == m.l:
            c' = max(c_local, m.c) + 1
        else:
            c' = 0
        (Guarantees monotonic coordinate advance while keeping pace with real time)
        ```

---

### How is Read-Your-Own-Writes consistency implemented in an eventually consistent distributed system?

??? question "Reveal answer"
    **Short Answer:** In an eventually consistent system with asynchronous replication, a client that writes data to the primary node and immediately reads from a lagging read replica will see stale state, appearing as if their write disappeared. **Read-Your-Own-Writes (RYOW)** consistency guarantees that a user always observes their own updates, while other concurrent users may observe eventual consistency.

    **Implementation Patterns:**
    1. **Primary Read Window:** Route all read requests from the updating user to the primary database for a brief window (e.g., 5 seconds) after any write operation.
    2. **Client-Side Version Tokens:** The server returns a replication LSN or transaction version token upon write. Subsequent read requests include this token in an HTTP header; the read replica delays response until its local replication offset catches up to the token (`WAIT_FOR_LSN`).
    3. **Optimistic Local Caching:** The client UI merges recent local mutations into its in-memory view until server responses confirm replication.

    ??? example "Example"
        ```http
        # Write Response returns replication commit token
        HTTP/1.1 200 OK
        X-Replication-Token: lsn:0/1A2B3C4D

        # Subsequent Read Request sends token to replica
        GET /api/v1/profile HTTP/1.1
        X-Wait-Until-LSN: lsn:0/1A2B3C4D
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### Incident: A 12-second Stop-The-World GC pause causes a worker's distributed lock lease to expire, resulting in dual writers and data corruption. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** Worker 1 acquired a Redis lock with a 10-second TTL to generate an analytics report file in S3. A sudden JVM heap allocation burst triggered a 12-second Stop-The-World GC pause. At $t=10\text{s}$, Redis expired the lock and granted it to Worker 2. At $t=12\text{s}$, Worker 1 resumed, assumed it still owned the lock, and uploaded its file, corrupting the upload in progress by Worker 2.
    
    **Remediation:**
    1. **Immediate Lock Lease Watchdog**: Integrate a lock renewal thread (e.g. Redisson watchdog) that renews the TTL periodically while the worker thread is active.
    2. **Fencing Token Implementation**: Return an atomic monotonic integer counter from Redis (`INCR lock:fencing:counter`). Include this token in the S3 metadata header.
    3. **Storage Verification**: Verify at the database/storage manifest level that writes with an older fencing token are rejected with HTTP 409 Conflict.

    ??? example "Example"
        ```java
        // Safe Fencing Token Execution Pattern
        long fencingToken = redisClient.incr("lock:fencing:counter");
        try {
            uploadService.uploadWithFencing(partitionId, data, fencingToken);
        } catch (StaleFencingTokenException ex) {
            logger.error("Lock lease was lost during processing; write rejected safely.");
        }
        ```

### Incident: A cross-switch network partition splits a 5-node cluster into a 3-node group and a 2-node group. Diagnose how quorum consensus handles writes in both partitions.

??? question "Reveal answer"
    **Short Answer:** In a 5-node cluster using Raft consensus, a strict majority requires $\lfloor 5/2 \rfloor + 1 = 3$ nodes.
    - **Group A (3 nodes)**: Retains quorum. It elects/maintains a leader and processes client writes normally because it can achieve majority consensus across 3 nodes.
    - **Group B (2 nodes)**: Loses quorum. If a leader was in this partition, it cannot achieve consensus for new log entries. The leader steps down, and nodes reject incoming writes with a cluster unavailable error.
    - **Reconnection**: When the network partition heals, Group B nodes discover Group A's higher Raft term, update their state machines from Group A's leader log, and resume normal operation with zero split-brain data divergence.

    ??? example "Example"
        ```text
        Total Nodes: N = 5
        Quorum Majority Required: 3 nodes
        Partition Event:
        - Sub-cluster 1: {Node 1, Node 2, Node 3} -> Size 3 >= 3 -> HEALTHY (Leader active, writes commit)
        - Sub-cluster 2: {Node 4, Node 5}          -> Size 2 < 3  -> READ-ONLY / BLOCKED (Writes rejected)
        ```

---

### Incident: Cascading cluster collapse caused by unjittered retry storms during database failover

A database primary underwent a planned 30-second maintenance restart. Instead of smoothly recovering, downstream application clusters suffered a 45-minute cascading outage. Walk through the failure dynamics and retry budget fix.

??? question "Reveal answer"
    **Short Answer:** When the database restarted, 150 application instances experienced connection errors. All instances were configured with standard retries on a fixed 1,000ms delay without jitter. When the database opened its TCP socket, 60,000 concurrent connection requests hit the server in synchronized waves every second. HikariCP connection timeouts flooded the database CPU to 100%, causing health check failures and continuous kernel OOM kills.

    **Remediation:**
    1. **Full Jitter Exponential Backoff:** Replaced fixed retry delays with exponential backoff with full jitter: $\text{sleep} = \text{random}(0, \min(M, B \times 2^i))$, flattening synchronized traffic spikes into a uniform distribution.
    2. **Retry Budgets:** Configured client-side retry budgets capping retry traffic to at most 10% of total request volume; if retry volume exceeds 10%, requests fail fast immediately.
    3. **Resilience4j Circuit Breaker:** Open circuit breaker on initial connection failure to prevent applications from hammering the database during startup.

    ??? example "Example"
        ```java
        // Exponential Backoff with Full Jitter calculation
        long baseSleepMs = 100;
        long maxSleepMs = 5000;
        long exponentialDelay = Math.min(maxSleepMs, baseSleepMs * (1L << attempt));
        long sleepWithJitter = ThreadLocalRandom.current().nextLong(0, exponentialDelay);
        Thread.sleep(sleepWithJitter);
        ```

---

### Incident: Silent financial ledger divergence caused by NTP clock step backward in Cassandra cluster

A financial payment microservice running on Cassandra experienced silent data corruption where customer balance updates were completely ignored after a VM live migration. Walk through the timestamp hazard and fix.

??? question "Reveal answer"
    **Short Answer:** Apache Cassandra relies on **Last-Write-Wins (LWW)** conflict resolution based on message timestamps. During a cloud VM live migration, the host system clock jumped backwards by 8 seconds before NTP stepped it into synchronization. Updates written by microservices during this 8-second window carried timestamps 8 seconds in the past. Because Cassandra had already committed prior balance updates with newer timestamps, Cassandra silently discarded the new valid transactions as "older" writes without returning an error!

    **Remediation:**
    1. **NTP Slew vs Step:** Configure time synchronization (via `chrony`) to use clock *slewing* rather than stepping, preventing system time from ever moving backwards.
    2. **Lightweight Transactions (Paxos LWT):** For financial ledger entries where ordering is critical, use Cassandra Paxos-based Lightweight Transactions (`IF EXISTS` / `IF balance = :expected`) instead of raw LWW timestamps.
    3. **Append-Only Immutable Event Sourcing:** Replace in-place mutable balance overwrites with an immutable transaction event stream, removing dependency on wall-clock order for ledger validity.

    ??? example "Example"
        ```cql
        -- Replace blind LWW update with Paxos Lightweight Transaction
        UPDATE account_balances 
        SET balance = 1500.00, version = 4 
        WHERE account_id = 'ACC-9821' 
        IF version = 3;
        ```
<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Production](production.md)
