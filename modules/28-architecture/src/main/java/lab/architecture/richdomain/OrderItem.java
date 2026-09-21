package lab.architecture.richdomain;

import java.util.Objects;

public record OrderItem(String productId, Quantity quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "ProductId must not be null");
        Objects.requireNonNull(quantity, "Quantity must not be null");
        Objects.requireNonNull(unitPrice, "UnitPrice must not be null");
        if (productId.isBlank()) {
            throw new IllegalArgumentException("ProductId must not be blank");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
