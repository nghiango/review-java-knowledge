package lab.restapi.questions;

import java.util.ArrayList;
import java.util.List;

public class Q14BulkAndBatchOperations {

    public record BulkItemResult(String itemId, int statusCode, String message) {}

    public static void main(String[] args) {
        List<String> requestedIds = List.of("item-1", "item-2", "item-3");
        List<BulkItemResult> results = new ArrayList<>();

        for (String id : requestedIds) {
            if ("item-2".equals(id)) {
                // Partial failure for item-2
                results.add(new BulkItemResult(id, 422, "Insufficient inventory"));
            } else {
                results.add(new BulkItemResult(id, 200, "Updated successfully"));
            }
        }

        // In batch operations with mixed outcomes, RFC 4918 defines HTTP 207 Multi-Status
        int multiStatus = 207;
        boolean hasFailures = results.stream().anyMatch(r -> r.statusCode() >= 400); // true
        long successCount = results.stream().filter(r -> r.statusCode() == 200).count(); // 2

        System.out.println("HTTP Status for batch: " + multiStatus); // HTTP Status for batch: 207
        System.out.println(
                "Batch has partial failures: " + hasFailures); // Batch has partial failures: true
        System.out.println("Successful items: " + successCount); // Successful items: 2
    }
}
