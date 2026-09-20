package lab.testing.fulfilment;

import java.util.Objects;

/**
 * Order fulfilment use case: decides whether the requested quantity is in stock.
 *
 * <p>The service depends on the typed {@link InventoryClient}, so it is written against {@link
 * InventoryResponse#available()} rather than against wire field names. It stays stateless and
 * performs no I/O of its own, which keeps it trivially unit-testable while the client boundary is
 * covered by a contract test.
 */
public final class OrderFulfilmentService {

    private final InventoryClient inventoryClient;

    public OrderFulfilmentService(InventoryClient inventoryClient) {
        this.inventoryClient =
                Objects.requireNonNull(inventoryClient, "inventoryClient must not be null");
    }

    /**
     * Returns whether {@code quantity} units of {@code sku} are currently available.
     *
     * @param sku the stock keeping unit; never {@code null}
     * @param quantity the requested quantity; must not be negative
     * @return {@code true} when the available stock covers the request
     * @throws IllegalArgumentException if {@code quantity} is negative
     */
    public boolean canFulfil(String sku, int quantity) {
        Objects.requireNonNull(sku, "sku must not be null");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        InventoryResponse response = inventoryClient.lookup(sku);
        return response.available() >= quantity;
    }
}
