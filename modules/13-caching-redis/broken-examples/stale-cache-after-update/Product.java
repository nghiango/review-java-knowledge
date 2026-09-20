package lab.cachingredis.broken.stalecache;

import java.math.BigDecimal;

public record Product(Long id, String name, BigDecimal price, int stockQuantity) {}
