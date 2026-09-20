# Concurrency Exercises

Hands-on concurrency katas to practice synchronization, lock-free patterns, and asynchronous coordination.

## Exercise 1: Build a Bounded Non-Blocking Ring Buffer with CAS

Implement a high-performance single-producer single-consumer ring buffer using atomic index pointers and power-of-two array indexing without `synchronized` or `ReentrantLock`.

### Requirements
- Fixed power-of-two capacity (e.g. 1024).
- `offer(E element)`: Returns `true` if inserted, `false` if buffer is full.
- `poll()`: Returns element if available, `null` if empty.
- Lock-free using atomic sequences or `AtomicLong`.

??? question "Reveal solution"
    ```java
    public class LockFreeRingBuffer<E> {
        private final Object[] buffer;
        private final int mask;
        private final AtomicLong head = new AtomicLong(0);
        private final AtomicLong tail = new AtomicLong(0);

        public LockFreeRingBuffer(int capacityPowerOfTwo) {
            this.buffer = new Object[capacityPowerOfTwo];
            this.mask = capacityPowerOfTwo - 1;
        }

        public boolean offer(E item) {
            long currentTail = tail.get();
            long currentHead = head.get();
            if (currentTail - currentHead >= buffer.length) {
                return false; // Full
            }
            buffer[(int) (currentTail & mask)] = item;
            tail.lazySet(currentTail + 1);
            return true;
        }

        @SuppressWarnings("unchecked")
        public E poll() {
            long currentHead = head.get();
            long currentTail = tail.get();
            if (currentHead >= currentTail) {
                return null; // Empty
            }
            int index = (int) (currentHead & mask);
            E item = (E) buffer[index];
            buffer[index] = null;
            head.lazySet(currentHead + 1);
            return item;
        }
    }
    ```

---

## Exercise 2: Implement a Scalable Rate Limiter with Token Bucket

Implement an in-memory token bucket rate limiter supporting concurrent requests.

### Requirements
- Capacity of $N$ tokens, refilling at $R$ tokens per second.
- `tryAcquire(int tokens)`: Returns `true` if sufficient tokens exist, atomically consuming them; otherwise `false`.
- Thread-safe using `AtomicLong` without holding locks across time calculation.

??? question "Reveal solution"
    ```java
    public class TokenBucketRateLimiter {
        private final long maxTokens;
        private final long refillTokensPerSecond;
        private final AtomicLong availableTokens;
        private final AtomicLong lastRefillTimestampNanos;

        public TokenBucketRateLimiter(long maxTokens, long refillTokensPerSecond) {
            this.maxTokens = maxTokens;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.availableTokens = new AtomicLong(maxTokens);
            this.lastRefillTimestampNanos = new AtomicLong(System.nanoTime());
        }

        public boolean tryAcquire(long tokens) {
            refill();
            while (true) {
                long current = availableTokens.get();
                if (current < tokens) {
                    return false;
                }
                if (availableTokens.compareAndSet(current, current - tokens)) {
                    return true;
                }
            }
        }

        private void refill() {
            long now = System.nanoTime();
            long lastRefill = lastRefillTimestampNanos.get();
            long elapsedNanos = now - lastRefill;
            long newTokens = (elapsedNanos * refillTokensPerSecond) / 1_000_000_000L;

            if (newTokens > 0 && lastRefillTimestampNanos.compareAndSet(lastRefill, now)) {
                availableTokens.updateAndGet(current -> Math.min(maxTokens, current + newTokens));
            }
        }
    }
    ```

## Related

- [Concepts](concepts.md)
- [Solutions](solutions.md)
- [Tests](tests.md)
