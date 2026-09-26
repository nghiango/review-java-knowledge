package lab.springsecurity.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q25: How does Spring Security prevent session fixation attacks and limit concurrent sessions
 * per user?
 */
public class Q25SessionFixationAndConcurrencyControl {

    public static void main(String[] args) {
        // Session fixation protection: changeSessionId() migrates attributes to a fresh session ID
        String preAuthSessionId = "anon-sess-001";
        String postAuthSessionId = "auth-sess-999";
        boolean sessionMigrated = !preAuthSessionId.equals(postAuthSessionId); // true

        // Concurrent session control: limit max 1 active session per user principal
        java.util.Map<String, String> userSessionRegistry = new java.util.HashMap<>();
        int maxSessions = 1;

        String user = "alice";
        userSessionRegistry.put(user, postAuthSessionId);

        // Attempting a second login from another device
        String secondLoginSessionId = "auth-sess-1000";
        boolean hasExistingSession = userSessionRegistry.containsKey(user); // true

        // Concurrency policy: expire older session or prevent new login
        if (hasExistingSession && userSessionRegistry.size() >= maxSessions) {
            String evictedSession = userSessionRegistry.put(user, secondLoginSessionId);
            System.out.println("Evicted Session ID: " + evictedSession); // auth-sess-999
        }

        boolean currentActiveIsLatest =
                secondLoginSessionId.equals(userSessionRegistry.get(user)); // true

        System.out.println("Session ID Migrated: " + sessionMigrated); // true
        System.out.println("Current Active Session: " + currentActiveIsLatest); // true
    }
}
