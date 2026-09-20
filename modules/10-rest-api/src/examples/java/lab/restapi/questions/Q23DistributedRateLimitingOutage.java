package lab.restapi.questions;

import org.springframework.http.HttpStatus;

public class Q23DistributedRateLimitingOutage {

    public static void main(String[] args) {
        // High traffic flash sale scenario: 10,000 requests/sec arriving at payment endpoint
        int requestCountInWindow = 120;
        int maxPermittedPerMinute = 100;

        boolean isRateLimited = requestCountInWindow > maxPermittedPerMinute; // true
        HttpStatus responseStatus =
                isRateLimited
                        ? HttpStatus.TOO_MANY_REQUESTS
                        : HttpStatus.OK; // HttpStatus.TOO_MANY_REQUESTS
        int retryAfterSeconds = 15;

        System.out.println("Rate limit breached: " + isRateLimited); // Rate limit breached: true
        System.out.println(
                "Returned HTTP status: " + responseStatus.value()); // Returned HTTP status: 429
        System.out.println(
                "Retry-After header value (seconds): "
                        + retryAfterSeconds); // Retry-After header value (seconds): 15
    }
}
