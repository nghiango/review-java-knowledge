package lab.restapi.questions;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("unused")
public final class Q27WebhookVsPollingAsyncPatternExample {
    private Q27WebhookVsPollingAsyncPatternExample() {}

    public record AsyncTaskResponse(String taskId, String status) {}

    // Pattern 1: Polling Status Pattern (HTTP 202 Accepted)
    // Client receives HTTP 202 with Location header pointing to task status endpoint and Retry-After header.
    public static ResponseEntity<AsyncTaskResponse> initiateLongRunningTask(String taskId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .location(URI.create("/api/v1/tasks/" + taskId))
            .header(HttpHeaders.RETRY_AFTER, "30")
            .body(new AsyncTaskResponse(taskId, "PROCESSING"));
    }

    // Pattern 2: Webhook Callback
    // Server asynchronously sends HTTP POST to client-provided callback URL with HMAC signature header:
    public static class WebhookDispatcher {
        public static String buildHmacHeader(String payload, String secret) {
            // Generates HMAC-SHA256 signature (e.g. X-Hub-Signature-256) so client can verify authenticity
            return "sha256=" + Integer.toHexString((payload + secret).hashCode());
        }
    }

    public static void main(String[] args) {
        ResponseEntity<AsyncTaskResponse> response = initiateLongRunningTask("task-9988");
        int status = response.getStatusCode().value(); // 202
        String location = response.getHeaders().getLocation().toString(); // "/api/v1/tasks/task-9988"

        String signature = WebhookDispatcher.buildHmacHeader("{\"event\":\"task.completed\"}", "secret-key");
        boolean hasSignature = signature.startsWith("sha256="); // true
    }
}
