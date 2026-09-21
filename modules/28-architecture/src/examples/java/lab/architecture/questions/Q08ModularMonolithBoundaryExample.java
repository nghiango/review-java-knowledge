package lab.architecture.questions;

/**
 * Q08: Monolith vs Modular Monolith vs Microservices. Demonstrates internal package encapsulation
 * in a modular monolith.
 */
public class Q08ModularMonolithBoundaryExample {

    // Module Public API contract
    public interface CustomerModuleApi {
        boolean isVipCustomer(String customerId);
    }

    // Module Implementation (can be internal / package-private)
    public static class CustomerModuleApiImpl implements CustomerModuleApi {
        @Override
        public boolean isVipCustomer(String customerId) {
            return customerId != null && customerId.startsWith("VIP-");
        }
    }

    public static void main(String[] args) {
        CustomerModuleApi api = new CustomerModuleApiImpl();

        boolean isVip = api.isVipCustomer("VIP-42"); // true
        boolean regular = api.isVipCustomer("REG-99"); // false

        System.out.println("Q08 isVip: " + isVip + ", regular: " + regular);
    }
}
