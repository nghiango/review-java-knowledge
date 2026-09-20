package lab.restapi.pagination;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItem(UUID id, String sku, String name, String category, BigDecimal price) {}
