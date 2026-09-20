package lab.restapi.questions;

public class Q21UnboundedListOomIncident {

    public static void main(String[] args) {
        int totalDatabaseRows = 1_000_000;
        int maxSafePageSize = 100;

        // Incident: Controller loads all 1,000,000 items into memory -> JVM Heap OOM & 30s latency
        int requestedUnboundedLimit = totalDatabaseRows;
        boolean causesMemorySpike = (requestedUnboundedLimit > maxSafePageSize); // true

        // Solution: Hard bounded page size cap
        int clientRequestedLimit = 500_000;
        int enforcedLimit = Math.min(clientRequestedLimit, maxSafePageSize); // 100
        boolean isSafeFromOom = (enforcedLimit <= maxSafePageSize); // true

        System.out.println(
                "Unbounded query causes memory spike: "
                        + causesMemorySpike); // Unbounded query causes memory spike: true
        System.out.println(
                "Enforced page size cap: " + enforcedLimit); // Enforced page size cap: 100
        System.out.println("Safe from OOM: " + isSafeFromOom); // Safe from OOM: true
    }
}
