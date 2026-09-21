package lab.designpatterns.decorator;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultOrderQueryService implements OrderQueryPort {

    private final Map<String, OrderSummary> database = new ConcurrentHashMap<>();

    public DefaultOrderQueryService() {
        database.put("ORD-1", new OrderSummary("ORD-1", "CUST-A", new BigDecimal("99.00"), false));
        database.put(
                "ORD-CONFIDENTIAL-99",
                new OrderSummary(
                        "ORD-CONFIDENTIAL-99", "VIP-SECRET", new BigDecimal("50000.00"), true));
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        return database.get(orderId);
    }
}
