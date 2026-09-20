package lab.springsecurity.questions;

import java.util.HashMap;
import java.util.Map;

public class Q17RateLimitingAndBruteForceProtection {

    public static void main(String[] args) {
        Map<String, Integer> failedLoginAttempts = new HashMap<>();
        int maxAllowedFailures = 5;

        String targetIp = "198.51.100.25";

        // Simulate 5 bad attempts
        for (int i = 0; i < 5; i++) {
            failedLoginAttempts.merge(targetIp, 1, Integer::sum);
        }

        boolean isBlockedAfter5 = failedLoginAttempts.get(targetIp) >= maxAllowedFailures; // true

        // 6th attempt is blocked immediately (HTTP 429 Too Many Requests)
        int currentCount = failedLoginAttempts.get(targetIp); // 5
        boolean allowNextAttempt = currentCount < maxAllowedFailures; // false

        System.out.println(
                "IP address locked out: " + isBlockedAfter5); // IP address locked out: true
        System.out.println(
                "New attempt permitted: " + allowNextAttempt); // New attempt permitted: false
    }
}
