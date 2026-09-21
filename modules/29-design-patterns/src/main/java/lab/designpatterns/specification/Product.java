package lab.designpatterns.specification;

import java.math.BigDecimal;

public record Product(
        String sku, String name, BigDecimal price, int stockQuantity, boolean active) {}
