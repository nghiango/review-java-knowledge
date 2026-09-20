package lab.resilience.questions;

import java.util.concurrent.atomic.AtomicInteger;

public class Q07RateLimiterTokenBucketVsLeakyBucketExample {

    record TokenBucket(int capacity, AtomicInteger tokens) {
        boolean tryConsume() {
            int current = tokens.get();
            if (current > 0) {
                return tokens.compareAndSet(current, current - 1);
            }
            return false;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Token bucket allows bursts up to capacity
        TokenBucket bucket = new TokenBucket(10, new AtomicInteger(2));

        boolean firstRequest = bucket.tryConsume(); // true
        boolean secondRequest = bucket.tryConsume(); // true
        boolean thirdRequestExceeds = !bucket.tryConsume(); // true

        System.out.println(
                "First: "
                        + firstRequest
                        + ", Second: "
                        + secondRequest
                        + ", Third rate-limited: "
                        + thirdRequestExceeds);
    }
}
