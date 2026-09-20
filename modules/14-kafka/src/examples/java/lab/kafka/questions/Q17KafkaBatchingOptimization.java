package lab.kafka.questions;

/**
 * Q17: How do linger.ms, batch.size, and compression.type drastically increase producer throughput?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q17KafkaBatchingOptimization {

    public static void main(String[] args) {
        // linger.ms: Artificial delay (e.g. 5-20ms) waiting for more records to accumulate into a
        // batch
        int lingerMs = 20;

        // batch.size: Max buffer memory in bytes allocated per partition batch (e.g. 64KB - 128KB)
        int batchSizeBytes = 65536; // 64 KB

        // Whichever threshold is hit first triggers network dispatch to broker
        boolean sendsWhenEitherThresholdReached = true; // true

        // compression.type: lz4 or snappy offer optimal CPU-to-compression ratio
        // Batch compression compresses the entire batch as one block, yielding much higher
        // compression
        // ratios than single-record compression and reducing broker network I/O
        String compressionType = "lz4";
        boolean compressesEntireBatchAtOnce = true; // true
    }
}
