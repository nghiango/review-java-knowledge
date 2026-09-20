package lab.jpahibernate.nplusone;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final EntityManager entityManager;

    public OrderService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Fix 1: JOIN FETCH loads all orders and their items in a single query, avoiding the N+1 select
     * problem while maintaining entity management.
     */
    @Transactional(readOnly = true)
    public List<Order> findAllOrdersWithItemsJoinFetch() {
        return entityManager
                .createQuery("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items", Order.class)
                .getResultList();
    }

    /**
     * Fix 2: DTO Projection selects only aggregate scalar values directly from the database,
     * avoiding entity lifecycle overhead and eliminating N+1 queries entirely.
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryDto> findOrderSummariesDtoProjection() {
        return entityManager
                .createQuery(
                        "SELECT new lab.jpahibernate.nplusone.OrderSummaryDto("
                                + "o.id, o.customerName, CAST(COUNT(i.id) AS int), COALESCE(SUM(i.price), 0)) "
                                + "FROM Order o LEFT JOIN o.items i "
                                + "GROUP BY o.id, o.customerName",
                        OrderSummaryDto.class)
                .getResultList();
    }
}
