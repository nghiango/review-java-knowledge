# Solution: Cross-Module Database Access in Modular Monolith

## Annotated Code

### `BillingService.java`

```java
package lab.architecture.broken.crossmodule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    // Maintainability issue: Direct cross-module repository injection violates bounded context boundaries
    private final InventoryRepository inventoryRepository;
    // Maintainability issue: Tight coupling to external module's persistence layer prevents independent schema evolution
    private final ShippingRepository shippingRepository;

    public BillingService(InventoryRepository inventoryRepository, ShippingRepository shippingRepository) {
        this.inventoryRepository = inventoryRepository;
        this.shippingRepository = shippingRepository;
    }

    public BigDecimal processBilling(Long orderId, String customerId, BigDecimal baseAmount) {
        // Maintainability issue: Bypassing inventory module public API to query raw database tables
        int stockCount = inventoryRepository.findStockCountByOrderId(orderId);
        if (stockCount <= 0) {
            throw new IllegalStateException("Cannot bill order with no allocated inventory");
        }

        // Maintainability issue: Direct cross-module read couples billing to internal shipment schema representation
        Map<String, Object> shipmentData = shippingRepository.findShipmentRecord(orderId);
        BigDecimal shippingCost = (BigDecimal) shipmentData.getOrDefault("cost", BigDecimal.ZERO);

        BigDecimal finalTotal = baseAmount.add(shippingCost);

        // Reliability issue: Direct cross-module mutation bypasses shipping domain validation, events, and lifecycle rules
        shippingRepository.updateShipmentStatus(orderId, "BILLING_COMPLETED");

        return finalTotal;
    }
}
```

## Issue Catalogue

1. **Violation of Bounded Context Boundaries**:
   - In Domain-Driven Design, each bounded context (`billing`, `shipping`, `inventory`) must strictly own its internal data schema and models.
   - Injecting foreign repositories turns a intended modular architecture into a "shared-database spaghetti monolith".
2. **Fragile Database Schema Migrations**:
   - If the Shipping team decides to refactor `shipments` table columns or migrate to an event-sourced model, the Billing module breaks unpredictably at runtime.
3. **Bypassed Domain Invariants**:
   - `shippingRepository.updateShipmentStatus(...)` updates a database record without going through the Shipping aggregate root. Shipping lifecycle validations and event publications are completely skipped.
4. **Impossible Extraction to Microservices**:
   - Shared database access prevents any module from ever being scaled independently or extracted into an autonomous microservice.

## Correct Implementation

The production-grade modular monolith architecture enforces **Module Boundary Isolation**:
- Bounded Context API Contracts: `lab.architecture.modularmonolith.inventory.InventoryApi` exposes a public, stable Java interface. The internal `InventoryRepository` remains package-private.
- Asynchronous Domain Events: Rather than `BillingService` mutating shipping records directly, billing publishes an `OrderBilledEvent`. The `ShippingModule` listens to this event in its own transaction and updates its state cleanly through its own aggregate root.
- ArchUnit Architectural Verification: Automated ArchUnit tests assert that classes in `lab.architecture.modularmonolith.billing..` cannot access package-private or repository classes in `..inventory..` or `..shipping..`.
