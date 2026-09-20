package lab.cachingredis.questions;

import java.util.UUID;

/**
 * Q13: How is an atomic distributed lock implemented in Redis using SET key value NX PX, and why is
 * Lua required for unlocking?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q13DistributedLockSetnx {

    public static void main(String[] args) {
        // Step 1: Lock acquisition must be atomic.
        // Command: SET lock:order:1001 <random_token> NX PX 10000
        // - NX: Only set if key does NOT exist
        // - PX 10000: Auto-expire after 10 seconds (prevents deadlocks if holder crashes)
        // - random_token: Unique identifier (e.g. UUID) identifying the lock owner
        String lockKey = "lock:order:1001";
        String ownerToken = UUID.randomUUID().toString();
        long leaseTimeMs = 10_000;

        boolean acquired = ownerToken != null; // true

        // Step 2: Unlocking MUST be atomic via Lua script.
        // If we use: GET token -> check equals -> DEL key:
        // A garbage collection pause between GET and DEL could cause lock expiration, allowing
        // another worker
        // to acquire the lock. The delayed DEL would then mistakenly delete the OTHER worker's
        // lock!
        String luaUnlockScript =
                """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        boolean luaEnsuresAtomicCheckAndDelete =
                luaUnlockScript.contains("redis.call('del'"); // true
    }
}
