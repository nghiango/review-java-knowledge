# Distributed Systems Exercises

Hands-on algorithmic and architectural exercises to master fencing token validation, causal vector clocks, and distributed concurrency controls.

---

## Exercise 1: Implement an Atomic Fencing Token Storage Gate

### Problem Statement

Design a thread-safe Java service that protects shared storage writes using distributed fencing tokens. 
When a worker acquires a lock, it receives an atomic, monotonically increasing 64-bit integer token. The storage gatekeeper must:
1. Accept the write only if the incoming fencing token is strictly greater than the highest fencing token ever committed for that resource.
2. If the token is less than or equal to the committed token, reject the write with a `StaleFencingTokenException`.
3. Update the latest token and payload atomically.

### Requirements
- Provide an atomic conditional update using `AtomicReference` (in-memory model) or SQL `UPDATE ... WHERE last_fencing_token < :token`.
- Simulate a delayed write from a paused worker and verify that the gatekeeper rejects it.

??? question "Reveal solution"
    ```java
    // Fencing Token Storage Gatekeeper
    class FencingStorageGatekeeper {

        private record StoredRecord(long fencingToken, String data) {}

        private final AtomicReference<StoredRecord> storage =
                new AtomicReference<>(new StoredRecord(0, "INITIAL_STATE"));

        public void commitWrite(long incomingToken, String newData) {
            while (true) {
                StoredRecord current = storage.get();

                // Fencing check: incoming token must be strictly greater than last committed token
                if (incomingToken <= current.fencingToken()) {
                    throw new StaleFencingTokenException(String.format(
                            "Rejected stale write! Incoming token %d <= last committed token %d",
                            incomingToken, current.fencingToken()));
                }

                StoredRecord updated = new StoredRecord(incomingToken, newData);
                if (storage.compareAndSet(current, updated)) {
                    // Atomically committed
                    return;
                }
            }
        }

        public StoredRecord readLatest() {
            return storage.get();
        }

        static class StaleFencingTokenException extends RuntimeException {
            public StaleFencingTokenException(String message) {
                super(message);
            }
        }
    }
    ```

---

## Exercise 2: Implement a Vector Clock Causality Detector

### Problem Statement

Implement a Vector Clock data structure in Java to track causal relationships across distributed nodes.

Each vector clock is represented as a mapping of node IDs to monotonic logical counters (`Map<String, Long>`).

Implement the following operations:
1. `increment(String nodeId)`: Increments the local node's counter.
2. `merge(VectorClock incoming)`: Updates the local vector clock by taking the maximum counter for each known node.
3. `compareTo(VectorClock other)`: Returns:
   - `HAPPENED_BEFORE` if this vector clock causally preceded the other ($V_A < V_B$).
   - `HAPPENED_AFTER` if this vector clock causally followed the other ($V_A > V_B$).
   - `EQUAL` if both vector clocks are identical ($V_A = V_B$).
   - `CONCURRENT` if neither vector clock dominates the other ($V_A \parallel V_B$, conflict detected!).

??? question "Reveal solution"
    ```java
    // Vector Clock Implementation and Causality Detector
    class VectorClock {

        public enum Causality {
            HAPPENED_BEFORE,
            HAPPENED_AFTER,
            EQUAL,
            CONCURRENT
        }

        private final Map<String, Long> clock = new ConcurrentHashMap<>();

        public VectorClock() {}

        public VectorClock(Map<String, Long> initialClock) {
            this.clock.putAll(initialClock);
        }

        public void increment(String nodeId) {
            clock.merge(nodeId, 1L, Long::sum);
        }

        public void merge(VectorClock incoming) {
            for (Map.Entry<String, Long> entry : incoming.clock.entrySet()) {
                clock.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }

        public Causality compareCausality(VectorClock other) {
            Set<String> allNodes = new HashSet<>();
            allNodes.addAll(this.clock.keySet());
            allNodes.addAll(other.clock.keySet());

            boolean hasGreater = false;
            boolean hasLesser = false;

            for (String node : allNodes) {
                long v1 = this.clock.getOrDefault(node, 0L);
                long v2 = other.clock.getOrDefault(node, 0L);

                if (v1 > v2) hasGreater = true;
                if (v1 < v2) hasLesser = true;
            }

            if (hasGreater && hasLesser) {
                return Causality.CONCURRENT; // Conflict! Uncoordinated concurrent writes.
            } else if (hasGreater) {
                return Causality.HAPPENED_AFTER;
            } else if (hasLesser) {
                return Causality.HAPPENED_BEFORE;
            } else {
                return Causality.EQUAL;
            }
        }

        public Map<String, Long> getSnapshot() {
            return Collections.unmodifiableMap(clock);
        }
    }
    ```

---

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Production](production.md)
