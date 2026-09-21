package lab.architecture.cleanarchitecture.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value must not be null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String uuidStr) {
        return new OrderId(UUID.fromString(uuidStr));
    }
}
