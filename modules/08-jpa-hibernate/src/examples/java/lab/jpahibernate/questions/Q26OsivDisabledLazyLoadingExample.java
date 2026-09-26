package lab.jpahibernate.questions;

import java.util.List;

@SuppressWarnings("unused")
public final class Q26OsivDisabledLazyLoadingExample {
    private Q26OsivDisabledLazyLoadingExample() {}

    public record OrderItemDto(String name, int quantity) {}
    public record OrderResponse(Long id, List<OrderItemDto> items) {}

    // Simulated Entity state
    public static class OrderEntity {
        private final Long id;
        private List<OrderItemDto> items;
        private boolean sessionOpen = true;

        public OrderEntity(Long id, List<OrderItemDto> items) {
            this.id = id;
            this.items = items;
        }

        public Long getId() { return id; }

        public List<OrderItemDto> getItems() {
            if (!sessionOpen) {
                // With spring.jpa.open-in-view: false, accessing lazy collection outside transaction throws:
                throw new RuntimeException("LazyInitializationException: could not initialize proxy - no Session");
            }
            return items;
        }

        public void closeSession() {
            this.sessionOpen = false;
        }
    }

    // Solution 1: Map entity to DTO INSIDE the transactional service boundary using JOIN FETCH / EntityGraph
    public static class TransactionalOrderService {
        public OrderResponse getOrderAsDto(OrderEntity entity) {
            // Transaction is active; accessing lazy collection initializes it cleanly:
            List<OrderItemDto> dtos = entity.getItems().stream()
                .map(item -> new OrderItemDto(item.name(), item.quantity()))
                .toList();

            // Session closes when transaction commits:
            entity.closeSession();

            // Return immutable DTO response - completely detached from JPA Session lifecycle!
            return new OrderResponse(entity.getId(), dtos);
        }
    }

    public static void main(String[] args) {
        OrderEntity entity = new OrderEntity(1L, List.of(new OrderItemDto("Widget", 2)));
        TransactionalOrderService service = new TransactionalOrderService();

        OrderResponse response = service.getOrderAsDto(entity);
        boolean success = response.items().size() == 1; // true (DTO mapping safely completed within tx)
    }
}
