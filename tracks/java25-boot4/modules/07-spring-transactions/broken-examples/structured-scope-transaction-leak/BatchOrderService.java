package lab.java25boot4.springtransactions.broken.structuredscope;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchOrderService {

    private final OrderItemRepository itemRepository;

    public BatchOrderService(OrderItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void processBatchOrders(List<String> orderIds) throws Exception {
        try (var scope = StructuredTaskScope.open()) {
            for (String orderId : orderIds) {
                scope.fork(() -> {
                    itemRepository.insertItem(orderId, "ITEM-DATA");
                    return null;
                });
            }
            scope.join();
        }

        if (orderIds.contains("fail-batch")) {
            throw new IllegalStateException("Simulated batch failure");
        }
    }

    public interface OrderItemRepository {
        void insertItem(String orderId, String itemData);
    }
}
