package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q25: How do Redis Keyspace Notifications work for expired events, and what are their limitations?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q25KeyspaceNotificationsExpiredEvents {

    public static void main(String[] args) {
        // Configuration requirement:
        // redis.conf -> notify-keyspace-events "Ex" (E = Keyevent events, x = Expired events)
        // Subscribes to Pub/Sub channel: __keyevent@<db>__:expired
        String channel = "__keyevent@0__:expired";
        boolean listensOnDb0ExpiredChannel = channel.contains("expired"); // true

        // Critical Production Limitations:
        // 1. Not Real-Time: Redis only fires the expired event when the key is actively sampled
        //    or lazily accessed upon read. A key with a 10s TTL might fire its expired notification
        //    seconds or minutes later if keyspace is cold.
        // 2. At-Most-Once Delivery (Pub/Sub): Pub/Sub messages are not queued. If the Spring Boot
        //    subscriber restarts or encounters a network hiccup, expired events are permanently lost.
        // 3. No Payload: The notification payload contains ONLY the key name, not the expired value.
        Map<String, Boolean> tradeOffs =
                Map.of(
                        "guaranteedRealTime", false,
                        "durableAcrossRestarts", false,
                        "containsOriginalValue", false);

        boolean isReliableForDistributedJobScheduling =
                tradeOffs.get("guaranteedRealTime") && tradeOffs.get("durableAcrossRestarts"); // false
    }
}
