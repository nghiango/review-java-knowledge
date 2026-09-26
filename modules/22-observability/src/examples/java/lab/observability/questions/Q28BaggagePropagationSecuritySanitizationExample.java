package lab.observability.questions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Demonstrates Baggage API propagation across distributed boundaries and security/PII sanitization
 * to prevent leaking sensitive credentials or unbounded metadata downstream.
 */
public class Q28BaggagePropagationSecuritySanitizationExample {

    public static class BaggageManager {
        private static final Set<String> ALLOWED_BAGGAGE_KEYS = Set.of("tenant_id", "feature_flags", "client_version");

        public Map<String, String> sanitizeForOutboundHeader(Map<String, String> rawBaggage) {
            Map<String, String> sanitized = new HashMap<>();
            for (Map.Entry<String, String> entry : rawBaggage.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                // Filter out non-whitelisted keys (e.g. auth tokens, emails, PII)
                if (ALLOWED_BAGGAGE_KEYS.contains(key)) {
                    // Truncate length to prevent HTTP header bloat
                    String value = entry.getValue();
                    if (value != null && value.length() > 64) {
                        value = value.substring(0, 64);
                    }
                    sanitized.put(key, value);
                }
            }
            return sanitized;
        }
    }

    public static void main(String[] args) {
        BaggageManager manager = new BaggageManager();
        Map<String, String> requestBaggage = Map.of(
                "tenant_id", "tenant-corp-123",
                "auth_jwt", "bearer.eyJhbGciOi...",
                "user_ssn", "123-45-6789"
        );

        Map<String, String> outboundBaggage = manager.sanitizeForOutboundHeader(requestBaggage);

        boolean containsTenant = outboundBaggage.containsKey("tenant_id"); // true
        boolean leaksJwt = outboundBaggage.containsKey("auth_jwt"); // false
        boolean leaksSsn = outboundBaggage.containsKey("user_ssn"); // false

        System.out.println("Tenant ID propagated: " + containsTenant);
        System.out.println("Sensitive JWT stripped: " + !leaksJwt);
        System.out.println("Sensitive SSN stripped: " + !leaksSsn);
    }
}
