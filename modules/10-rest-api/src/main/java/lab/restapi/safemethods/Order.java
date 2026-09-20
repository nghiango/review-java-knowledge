package lab.restapi.safemethods;

import java.util.UUID;

public record Order(UUID id, String customerId, OrderStatus status) {}
