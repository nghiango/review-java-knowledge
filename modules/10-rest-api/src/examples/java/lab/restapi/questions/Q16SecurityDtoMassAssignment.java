package lab.restapi.questions;

import java.util.Map;

public class Q16SecurityDtoMassAssignment {

    // Vulnerable pattern: binding incoming JSON payload directly to persistent Entity
    public static class VulnerableEntity {
        public String username;
        public String role = "USER";
        public boolean isAdmin = false;
    }

    // Secure pattern: strict DTO specifying only allowed mutable fields
    public record SafeUpdateDto(String displayName) {}

    public static void main(String[] args) {
        // Attacker sends extra administrative fields in request body
        Map<String, Object> maliciousPayload =
                Map.of(
                        "displayName", "Attacker",
                        "role", "ADMIN",
                        "isAdmin", true);

        // Vulnerable binding: attacker escalates privileges
        VulnerableEntity entity = new VulnerableEntity();
        if (maliciousPayload.containsKey("role")) {
            entity.role = (String) maliciousPayload.get("role");
        }
        boolean privilegeEscalated = "ADMIN".equals(entity.role); // true

        // Safe DTO binding: ignores/drops unauthorized fields
        SafeUpdateDto safeDto = new SafeUpdateDto((String) maliciousPayload.get("displayName"));
        boolean rolePreservedSafe = "Attacker".equals(safeDto.displayName()); // true

        System.out.println(
                "Vulnerable entity exploited: "
                        + privilegeEscalated); // Vulnerable entity exploited: true
        System.out.println(
                "Safe DTO shielded internal state: "
                        + rolePreservedSafe); // Safe DTO shielded internal state: true
    }
}
