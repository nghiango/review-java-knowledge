package lab.restapi.statuscodes;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(UUID id, String sku, String name, BigDecimal price) {}
