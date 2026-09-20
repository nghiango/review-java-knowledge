package lab.springsecurity.questions;

import java.util.Map;
import java.util.UUID;

public class Q15IdorBolaPreventionPatterns {

    public static void main(String[] args) {
        UUID invoiceId = UUID.randomUUID();
        Map<UUID, String> invoiceOwners = Map.of(invoiceId, "alice");

        String authenticatedUser = "bob"; // Attacker attempting to read Alice's invoice

        // Secure pattern: Data-driven ownership verification
        String resourceOwner = invoiceOwners.get(invoiceId);
        boolean isOwner = authenticatedUser.equals(resourceOwner); // false

        boolean accessGranted = isOwner; // false

        System.out.println("Resource Owner: " + resourceOwner); // Resource Owner: alice
        System.out.println("Authenticated User: " + authenticatedUser); // Authenticated User: bob
        System.out.println(
                "Access Granted (IDOR Prevented): "
                        + accessGranted); // Access Granted (IDOR Prevented): false
    }
}
