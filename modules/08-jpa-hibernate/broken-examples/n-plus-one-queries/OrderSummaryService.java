package lab.jpahibernate.broken.nplusone;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSummaryService {

    private final EntityManager entityManager;

    public OrderSummaryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public double calculateTotalRevenue() {
        // Query 1: Loads all 1000 orders
        List<Order> orders =
                entityManager
                        .createQuery("SELECT o FROM Order o", Order.class)
                        .getResultList();

        double total = 0.0;
        // Queries 2..1001: Accessing lazy items collection in a loop triggers N secondary queries!
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        return total;
    }
}
