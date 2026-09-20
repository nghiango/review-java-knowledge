package lab.rabbitmq.questions;

import java.util.List;

/**
 * Q17: How does the rabbitmq_consistent_hash_exchange enable horizontal partition-like queue
 * sharding?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q17ConsistentHashExchangeSharding {

    public static void main(String[] args) {
        // Standard RabbitMQ queues are bounded by a single Erlang process core (~30k-50k msgs/sec
        // limit).
        // To scale beyond a single core, use the Consistent Hash Exchange (x-consistent-hash).
        boolean scalesBeyondSingleCore = true; // true

        // Consistent Hash Routing:
        // Publishers supply a routing key (e.g. userId or orderId) or set hash-header.
        // Exchange hashes the key onto a consistent hash ring and routes to one of N bound sharded
        // queues:
        List<String> shardQueues =
                List.of("orders_shard_0", "orders_shard_1", "orders_shard_2", "orders_shard_3");
        int shardCount = shardQueues.size(); // 4

        // Preserves per-entity sequential ordering:
        // All messages with the same entity key consistently route to the SAME shard queue,
        // mimicking Kafka partition ordering!
        boolean providesKeyAffinityOrdering = true; // true
    }
}
