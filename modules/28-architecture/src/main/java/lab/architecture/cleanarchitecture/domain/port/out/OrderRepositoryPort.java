package lab.architecture.cleanarchitecture.domain.port.out;

import java.util.Optional;
import lab.architecture.cleanarchitecture.domain.model.Order;
import lab.architecture.cleanarchitecture.domain.model.OrderId;

/**
 * Outgoing port for Order persistence.
 * Defined by the domain; implemented by infrastructure adapters.
 */
public interface OrderRepositoryPort {

    void save(Order order);

    Optional<Order> findById(OrderId id);
}
