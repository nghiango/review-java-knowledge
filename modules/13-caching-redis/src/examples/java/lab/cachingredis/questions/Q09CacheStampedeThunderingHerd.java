package lab.cachingredis.questions;

import java.util.Map;

/** Q09: What is a Cache Stampede (Thundering Herd), and what strategies effectively prevent it? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q09CacheStampedeThunderingHerd {

    public static void main(String[] args) {
        // Cache Stampede: Occurs when a high-traffic cached key expires, causing hundreds of
        // concurrent
        // requests to simultaneously experience a cache miss and run expensive queries against the
        // DB.
        Map<String, String> defenseStrategies =
                Map.of(
                        "Distributed Mutex",
                        "Acquire single-flight lock via Redis SETNX. Only 1 worker recomputes; others wait or read stale.",
                        "Probabilistic Early Recomputation (XFetch)",
                        "Recompute before expiration probabilistically: -beta * delta * ln(random()) > (expiry - now)",
                        "Background Pre-warming",
                        "Scheduled cron periodically recomputes hot keys before expiration with infinite TTL.");

        boolean mutexBlocksThunderingHerd =
                defenseStrategies.get("Distributed Mutex").contains("Only 1 worker"); // true
        boolean xfetchRecomputesEarly =
                defenseStrategies
                        .get("Probabilistic Early Recomputation (XFetch)")
                        .contains("before expiration"); // true
    }
}
