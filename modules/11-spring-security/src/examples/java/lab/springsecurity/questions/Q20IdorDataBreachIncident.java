package lab.springsecurity.questions;

import java.util.Map;
import java.util.UUID;

public class Q20IdorDataBreachIncident {

    public static void main(String[] args) {
        UUID invoiceId = UUID.randomUUID();
        Map<UUID, String> dbInvoices = Map.of(invoiceId, "alice");

        String attackerUsername = "mallory";

        // Incident: Insecure endpoint returning record by ID without checking principal
        boolean vulnerableEndpointAccess =
                true; // Insecure endpoint leaked Alice's invoice to Mallory!

        // Remediation: Enforcing strict ownership check
        String owner = dbInvoices.get(invoiceId);
        boolean securedAccessAllowed = attackerUsername.equals(owner); // false (access denied)

        System.out.println(
                "Vulnerable endpoint leaked data: "
                        + vulnerableEndpointAccess); // Vulnerable endpoint leaked data: true
        System.out.println(
                "Secured endpoint allowed access: "
                        + securedAccessAllowed); // Secured endpoint allowed access: false
    }
}
