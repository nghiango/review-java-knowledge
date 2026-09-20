package lab.cachingredis.questions;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Q08: How does Redis reclaim expired keys (passive vs active expiration), and why is TTL jitter
 * critical?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q08RedisTtlAndKeyExpiration {

    public static void main(String[] args) {
        // 1. Passive Expiration (Lazy):
        // Redis checks a key's TTL when a client attempts to read it. If expired, it deletes the
        // key and returns nil.
        boolean deletedOnReadIfExpired = true; // true

        // 2. Active Expiration (Periodic):
        // Redis samples 20 random keys with an expiration 10 times per second.
        // If more than 25% of sampled keys are expired, it repeats the sampling cycle immediately.
        boolean periodicSamplingActive = true; // true

        // 3. TTL Jitter:
        // If thousands of keys are written at midnight with fixed TTL of 1 hour, all expire
        // simultaneously at 1:00 AM,
        // causing sudden CPU spikes in Redis and a database avalanche.
        long baseTtlSeconds = 3600; // 1 hour
        long jitterSeconds = ThreadLocalRandom.current().nextLong(-180, 180); // +/- 3 minutes
        long effectiveTtl = baseTtlSeconds + jitterSeconds;

        boolean isJittered = effectiveTtl >= 3420 && effectiveTtl <= 3780; // true
    }
}
