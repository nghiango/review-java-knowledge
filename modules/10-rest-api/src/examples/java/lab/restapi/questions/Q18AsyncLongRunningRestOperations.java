package lab.restapi.questions;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Q18AsyncLongRunningRestOperations {

    public record JobStatusResponse(String jobId, String status, int progressPercentage) {}

    public static void main(String[] args) {
        String jobId = "job-uuid-7788";
        URI statusLocation = URI.create("/api/reports/jobs/" + jobId);

        // Client initiates long-running task -> Server responds 202 Accepted + Location header
        ResponseEntity<Void> acceptedResponse =
                ResponseEntity.status(HttpStatus.ACCEPTED)
                        .location(statusLocation)
                        .header("Retry-After", "10")
                        .build();

        int statusCode = acceptedResponse.getStatusCode().value(); // 202
        URI locationHeader =
                acceptedResponse.getHeaders().getLocation(); // /api/reports/jobs/job-uuid-7788
        String retryAfter = acceptedResponse.getHeaders().getFirst("Retry-After"); // "10"

        // Subsequent poll returns job status
        JobStatusResponse status = new JobStatusResponse(jobId, "IN_PROGRESS", 45);
        boolean isInProgress = "IN_PROGRESS".equals(status.status()); // true

        System.out.println("Status Code: " + statusCode); // Status Code: 202
        System.out.println(
                "Location: " + locationHeader); // Location: /api/reports/jobs/job-uuid-7788
        System.out.println("Retry-After: " + retryAfter); // Retry-After: 10
        System.out.println("Job in progress: " + isInProgress); // Job in progress: true
    }
}
