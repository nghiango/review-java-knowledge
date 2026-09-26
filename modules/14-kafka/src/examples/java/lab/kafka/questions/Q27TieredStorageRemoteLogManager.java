package lab.kafka.questions;

import java.util.Map;

/**
 * Q27: How does Kafka Tiered Storage (Remote Storage Manager) decouple log retention from local NVMe broker storage?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q27TieredStorageRemoteLogManager {

    public static void main(String[] args) {
        // Traditional Kafka Storage Problem:
        // Storing weeks/months of data on local broker NVMe SSDs is cost-prohibitive.
        // Expanding storage requires adding broker nodes, which triggers massive network rebalancing.

        // Tiered Storage (KIP-405):
        // Splits partition log into two tiers:
        // 1. Local Tier (Fast NVMe): Stores recent active segments (e.g. last 2-4 hours).
        //    Absorbs 99% of real-time consumer traffic from OS page cache.
        // 2. Remote Tier (Object Storage - S3 / GCS / Azure Blob): Inactive, closed log segments
        //    are copied to object storage by the RemoteLogManager (RLM).
        //    Local broker deletes the cold segments, keeping local disk footprint small.

        Map<String, String> storageTiers =
                Map.of(
                        "Local Tier", "Fast NVMe disk; tail-read latency < 1ms; 2-4 hours retention",
                        "Remote Tier", "Cloud object storage; low-cost; infinite retention; zero rebalance overhead");

        boolean objectStorageAllowsDecoupledRetention =
                storageTiers.get("Remote Tier").contains("infinite retention"); // true
    }
}
