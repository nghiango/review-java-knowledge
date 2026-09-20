package lab.testing.fulfilment;

/**
 * Stock level for a single SKU as returned by the inventory service.
 *
 * <p>A typed value object rather than a raw {@code Map}: the wire field names are bound once, in
 * {@link InventoryClient}, so the rest of the domain never sees the transport format.
 *
 * @param sku the stock keeping unit
 * @param available units currently available
 */
public record InventoryResponse(String sku, int available) {}
