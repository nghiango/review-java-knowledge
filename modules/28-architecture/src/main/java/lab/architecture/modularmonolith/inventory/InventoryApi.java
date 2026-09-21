package lab.architecture.modularmonolith.inventory;

/**
 * Public contract exposed by the Inventory bounded context. External modules may only interact with
 * Inventory via this interface.
 */
public interface InventoryApi {

    boolean isAvailable(String productId, int quantity);

    boolean reserve(String productId, int quantity);
}
