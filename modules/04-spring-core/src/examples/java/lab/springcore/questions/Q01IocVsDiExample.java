package lab.springcore.questions;

/** Q01: Demonstrates Inversion of Control (IoC) and Dependency Injection (DI). */
@SuppressWarnings("unused")
public class Q01IocVsDiExample {

    interface OrderRepository {
        String findById(String id);
    }

    static class OrderService {
        private final OrderRepository repository;

        // Inversion of Control: dependency is injected externally rather than instantiated with
        // 'new'
        public OrderService(OrderRepository repository) {
            this.repository = repository;
        }

        public String getOrder(String id) {
            return repository.findById(id);
        }
    }

    public static void main(String[] args) {
        OrderRepository mockRepo = id -> "ORD-123";
        OrderService service = new OrderService(mockRepo);

        String order = service.getOrder("1"); // "ORD-123"
        boolean isDecoupled = service != null; // true
    }
}
