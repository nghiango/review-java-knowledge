# Solution: N+1 Query Problem

## Annotated Code

```java
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
        // Performance issue: initial query loads orders without fetching child collections
        List<Order> orders =
                entityManager
                        .createQuery("SELECT o FROM Order o", Order.class)
                        .getResultList();

        double total = 0.0;
        // Performance issue: accessing lazy items in loop triggers N separate SQL SELECT queries (N+1 query problem)
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        return total;
    }
}
```

## Issues Identified

### Performance issue: accessing lazy collection in loop triggers N+1 SQL queries
- **Location:** `OrderSummaryService.java#calculateTotalRevenue`
- **Explanation:** The initial query fetches $1$ result set of $N$ orders. When the loop accesses `order.getItems()`, Hibernate initializes the lazy proxy collection by executing an additional SQL `SELECT` for each order, resulting in $1 + N$ queries. For 10,000 orders, this executes 10,001 database queries, causing severe database load, network latency amplification, and connection pool saturation.

## Correct implementation

See `lab.jpahibernate.nplusone.OrderSummaryService` and `lab.jpahibernate.nplusone.OrderRepository` (using JPQL `JOIN FETCH` or aggregate DTO projection).
