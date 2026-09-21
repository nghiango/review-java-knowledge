package lab.architecture.cleanarchitecture.domain.model;

import java.util.Objects;

public record OrderItem(String productId, int quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "ProductId must not be null");
        Objects.requireNonNull(unitPrice, "UnitPrice must not be null");
        if (productId.isBlank()) {
            throw new IllegalArgumentException("ProductId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero: " + quantity);
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
