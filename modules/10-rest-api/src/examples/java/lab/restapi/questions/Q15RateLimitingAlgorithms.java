package lab.restapi.questions;

import java.util.concurrent.atomic.AtomicInteger;

public class Q15RateLimitingAlgorithms {

    public static class SimpleTokenBucket {
        private final AtomicInteger tokens;

        public SimpleTokenBucket(int capacity) {
            this.tokens = new AtomicInteger(capacity);
        }

        public synchronized boolean tryConsume() {
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        public int availableTokens() {
            return tokens.get();
        }
    }

    public static void main(String[] args) {
        SimpleTokenBucket bucket = new SimpleTokenBucket(2);

        boolean req1 = bucket.tryConsume(); // true
        boolean req2 = bucket.tryConsume(); // true
        boolean req3 =
                bucket.tryConsume(); // false (Rate limit exceeded -> HTTP 429 Too Many Requests)

        System.out.println("Req 1 allowed: " + req1); // Req 1 allowed: true
        System.out.println("Req 2 allowed: " + req2); // Req 2 allowed: true
        System.out.println("Req 3 allowed (HTTP 429): " + req3); // Req 3 allowed (HTTP 429): false
        System.out.println("Tokens remaining: " + bucket.availableTokens()); // Tokens remaining: 0
    }
}
