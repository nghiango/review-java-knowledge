package lab.java25boot4.resilience.questions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q06: How does a lock-free token-bucket or atomic permit limiter govern throughput under high
 * concurrency?
 */
public class Q06RateLimiterTokenBucketExample {

    static class SimpleAtomicTokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;

        SimpleAtomicTokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
        }

        public boolean tryConsume() {
            while (true) {
                int current = tokens.get();
                if (current <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        public void replenish(int count) {
            tokens.updateAndGet(existing -> Math.min(capacity, existing + count));
        }
    }

    public static void main(String[] args) {
        var bucket = new SimpleAtomicTokenBucket(2);

        boolean t1 = bucket.tryConsume();
        boolean t2 = bucket.tryConsume();
        boolean t3 = bucket.tryConsume();

        System.out.println("First token consumed: " + t1); // First token consumed: true
        System.out.println("Second token consumed: " + t2); // Second token consumed: true
        System.out.println("Third token consumed: " + t3); // Third token consumed: false

        bucket.replenish(1);
        System.out.println(
                "Token consumed after replenish: "
                        + bucket.tryConsume()); // Token consumed after replenish: true
    }
}
