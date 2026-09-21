package lab.architecture.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q25: Multi-Tenant Architecture Isolation Strategies. Demonstrates TenantContext thread-local
 * propagation and discriminator-based row isolation.
 */
public class Q25MultiTenantIsolationStrategiesExample {

    public static class TenantContextHolder {
        private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

        public static void setTenantId(String tenantId) {
            CURRENT_TENANT.set(tenantId);
        }

        public static String getTenantId() {
            return CURRENT_TENANT.get();
        }

        public static void clear() {
            CURRENT_TENANT.remove();
        }
    }

    public static class MultiTenantOrderStore {
        private final Map<String, String> dataStore = new HashMap<>();

        public void save(String orderId, String payload) {
            String tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Tenant context missing");
            }
            dataStore.put(tenantId + ":" + orderId, payload);
        }

        public String findById(String orderId) {
            String tenantId = TenantContextHolder.getTenantId();
            return dataStore.get(tenantId + ":" + orderId);
        }
    }

    public static void main(String[] args) {
        MultiTenantOrderStore store = new MultiTenantOrderStore();

        // Tenant A saves an order
        TenantContextHolder.setTenantId("tenant-alpha");
        store.save("ORD-1", "Alpha Order Data");

        // Tenant B cannot access Tenant A's order even with the same order ID
        TenantContextHolder.setTenantId("tenant-beta");
        String crossTenantAccess =
                store.findById("ORD-1"); // null (isolated by tenant discriminator)

        boolean dataSegregated = (crossTenantAccess == null); // true

        TenantContextHolder.clear();
        System.out.println(
                "Q27 crossTenantAccess: " + crossTenantAccess + ", segregated: " + dataSegregated);
    }
}
