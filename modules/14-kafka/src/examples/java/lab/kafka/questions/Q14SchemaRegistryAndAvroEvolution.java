package lab.kafka.questions;

import java.util.List;

/**
 * Q14: Why is a Schema Registry needed with Apache Avro/Protobuf, and how does schema compatibility
 * work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q14SchemaRegistryAndAvroEvolution {

    public static void main(String[] args) {
        // Wire format: 1 magic byte (0x00) + 4-byte Schema ID from Schema Registry + binary Avro
        // payload
        int magicByteLength = 1;
        int schemaIdLength = 4;
        int headerOverheadBytes = magicByteLength + schemaIdLength; // 5

        // Compatibility modes:
        // BACKWARD (default): Consumers with new schema can read data written with old schema (add
        // optional fields with default)
        // FORWARD: Consumers with old schema can read data written with new schema (delete optional
        // fields)
        // FULL: Both backward and forward compatible
        List<String> compatibilityModes = List.of("BACKWARD", "FORWARD", "FULL", "NONE");
        boolean defaultIsBackward = compatibilityModes.get(0).equals("BACKWARD"); // true

        // Schema Registry caches schemas locally to prevent HTTP latency on every message
        // write/read
        boolean schemasCachedLocally = true; // true
    }
}
