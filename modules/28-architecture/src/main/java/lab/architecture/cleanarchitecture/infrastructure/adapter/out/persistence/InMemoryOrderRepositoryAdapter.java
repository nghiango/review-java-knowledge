package lab.architecture.cleanarchitecture.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lab.architecture.cleanarchitecture.domain.model.Order;
import lab.architecture.cleanarchitecture.domain.model.OrderId;
import lab.architecture.cleanarchitecture.domain.port.out.OrderRepositoryPort;

/**
 * Infrastructure adapter implementing persistence port using an in-memory store.
 * In a real production environment, this would be a JPA / JDBC adapter.
 */
public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {

    private final Map<OrderId, Order> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        if (order != null) {
            storage.put(order.getId(), order);
        }
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(storage.get(id));
    }

    public int count() {
        return storage.size();
    }
}
