package lab.corejava.broken.streamprocessing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderReportService {
    public record Order(String orderId, String customerId) {}
    public record PricedOrder(Order order, BigDecimal price) {}

    private final PriceClient priceClient;

    public OrderReportService(PriceClient priceClient) {
        this.priceClient = priceClient;
    }

    public List<PricedOrder> generate(List<Order> orders) {
        var report = new ArrayList<PricedOrder>();
        orders.parallelStream().forEach(order -> {
            try {
                report.add(new PricedOrder(order, priceClient.lookup(order.orderId())));
            } catch (RuntimeException failure) {
                throw new RuntimeException("Pricing failed");
            }
        });
        return report;
    }
}
