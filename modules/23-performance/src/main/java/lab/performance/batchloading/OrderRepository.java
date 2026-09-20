package lab.performance.batchloading;

import java.util.List;

public interface OrderRepository {
    List<Order> findRecent(int limit);
}
