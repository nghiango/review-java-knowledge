package lab.corejava.questions;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class Q18EqualityContractEvolutionExample {
    private Q18EqualityContractEvolutionExample() {}

    public record CustomerKey(String tenantId, String customerId) {}

    public static void main(String[] args) {
        Set<CustomerKey> activeCustomers = new HashSet<>();
        activeCustomers.add(new CustomerKey("tenant-1", "cust-42"));

        // Comparing equality using consistent immutable composite record
        boolean exists =
                activeCustomers.contains(
                        new CustomerKey(
                                "tenant-1",
                                "cust-42")); // true (deduplication works reliably across versions)
        boolean diffTenant =
                activeCustomers.contains(new CustomerKey("tenant-2", "cust-42")); // false
    }
}
