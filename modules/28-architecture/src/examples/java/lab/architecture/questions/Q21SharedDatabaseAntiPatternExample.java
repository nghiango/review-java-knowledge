package lab.architecture.questions;

import java.util.Set;

/**
 * Q21: Shared Database Anti-Pattern in Microservices and Modular Monoliths.
 * Demonstrates why allowing multiple services to query a single shared table violates autonomy.
 */
public class Q21SharedDatabaseAntiPatternExample {

    public static class DatabaseSchemaGovernance {
        private final Set<String> tablesOwnedByBilling = Set.of("invoices", "payment_records");

        public boolean canServiceDirectlyQueryTable(String serviceName, String tableName) {
            if ("ShippingService".equals(serviceName) && tablesOwnedByBilling.contains(tableName)) {
                return false; // Forbidden: Shipping cannot directly query Billing's internal tables
            }
            return true;
        }
    }

    public static void main(String[] args) {
        DatabaseSchemaGovernance gov = new DatabaseSchemaGovernance();

        boolean allowed = gov.canServiceDirectlyQueryTable("BillingService", "invoices"); // true
        boolean forbidden = gov.canServiceDirectlyQueryTable("ShippingService", "invoices"); // false (prevents shared DB antipattern)

        System.out.println("Q21 allowed: " + allowed + ", forbidden: " + forbidden);
    }
}
