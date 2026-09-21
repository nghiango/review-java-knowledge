package lab.java25boot4.restapi.questions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Q13: Scenario: An e-commerce platform decommissioned API v1 without RFC 8594 headers, causing 35%
 * of mobile checkouts to fail. How does an RFC-compliant sunset strategy prevent outages?
 */
public class Q13ScenarioVersionSunsetOutageExample {

    public record ApiSunsetLifecycle(
            String version,
            Instant deprecationAnnounced,
            Instant sunsetHardDeadline,
            boolean isDecommissioned) {

        public boolean isDeprecationActive(Instant current) {
            return current.isAfter(deprecationAnnounced) && current.isBefore(sunsetHardDeadline);
        }

        public boolean shouldRejectWithSunsetError(Instant current) {
            return current.isAfter(sunsetHardDeadline) || isDecommissioned;
        }
    }

    public static void main(String[] args) {
        Instant now = Instant.now();
        Instant dep = now.minus(180, ChronoUnit.DAYS);
        Instant sunset = now.plus(30, ChronoUnit.DAYS);

        ApiSunsetLifecycle lifecycle = new ApiSunsetLifecycle("v1", dep, sunset, false);

        boolean inGracePeriod = lifecycle.isDeprecationActive(now);
        boolean hardRejected = lifecycle.shouldRejectWithSunsetError(now);

        System.out.println("API version: " + lifecycle.version()); // "v1"
        System.out.println("In sunset grace period: " + inGracePeriod); // true
        System.out.println("Hard cut-off applied: " + hardRejected); // false
    }
}
