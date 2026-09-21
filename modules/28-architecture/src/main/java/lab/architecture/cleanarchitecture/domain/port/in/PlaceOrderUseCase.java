package lab.architecture.cleanarchitecture.domain.port.in;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import lab.architecture.cleanarchitecture.domain.model.OrderId;

/** Incoming port (Use Case) defining order placement interactions. */
public interface PlaceOrderUseCase {

    PlaceOrderResult placeOrder(PlaceOrderCommand command);

    record ItemCommand(String productId, int quantity, BigDecimal unitPrice) {}

    record PlaceOrderCommand(
            String customerId, Currency currency, List<ItemCommand> items, String paymentToken) {}

    record PlaceOrderResult(OrderId orderId, boolean success, String message) {}
}
