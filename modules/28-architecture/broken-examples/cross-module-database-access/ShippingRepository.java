package lab.architecture.broken.crossmodule;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ShippingRepository {

    private final Map<Long, Map<String, Object>> shipments = new ConcurrentHashMap<>();

    public Map<String, Object> findShipmentRecord(Long orderId) {
        return shipments.computeIfAbsent(orderId, id -> {
            Map<String, Object> map = new HashMap<>();
            map.put("cost", new BigDecimal("15.00"));
            map.put("status", "PENDING");
            return map;
        });
    }

    public void updateShipmentStatus(Long orderId, String status) {
        Map<String, Object> record = shipments.computeIfAbsent(orderId, id -> new HashMap<>());
        record.put("status", status);
    }
}
