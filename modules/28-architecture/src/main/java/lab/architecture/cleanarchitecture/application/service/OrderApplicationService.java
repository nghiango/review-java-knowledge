package lab.architecture.cleanarchitecture.application.service;

import java.util.Objects;
import lab.architecture.cleanarchitecture.domain.model.Money;
import lab.architecture.cleanarchitecture.domain.model.Order;
import lab.architecture.cleanarchitecture.domain.model.OrderId;
import lab.architecture.cleanarchitecture.domain.model.OrderItem;
import lab.architecture.cleanarchitecture.domain.port.in.PlaceOrderUseCase;
import lab.architecture.cleanarchitecture.domain.port.out.OrderRepositoryPort;
import lab.architecture.cleanarchitecture.domain.port.out.PaymentPort;

/**
 * Application service orchestrating order placement use case.
 * Coordinates domain models and outgoing ports without holding business invariants.
 */
public class OrderApplicationService implements PlaceOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentPort paymentPort;

    public OrderApplicationService(OrderRepositoryPort orderRepositoryPort, PaymentPort paymentPort) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "OrderRepositoryPort must not be null");
        this.paymentPort = Objects.requireNonNull(paymentPort, "PaymentPort must not be null");
    }

    @Override
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "PlaceOrderCommand must not be null");

        OrderId orderId = OrderId.generate();
        Order order = new Order(orderId, command.customerId(), command.currency());

        for (ItemCommand itemCmd : command.items()) {
            Money unitPrice = new Money(itemCmd.unitPrice(), command.currency());
            OrderItem item = new OrderItem(itemCmd.productId(), itemCmd.quantity(), unitPrice);
            order.addItem(item);
        }

        PaymentPort.PaymentResult paymentResult = paymentPort.processPayment(
                order.getId(),
                order.getTotalAmount(),
                command.paymentToken()
        );

        if (paymentResult.successful()) {
            order.markPaid();
            orderRepositoryPort.save(order);
            return new PlaceOrderResult(orderId, true, "Order placed and paid successfully");
        } else {
            order.cancel("Payment rejected: " + paymentResult.errorMessage());
            orderRepositoryPort.save(order);
            return new PlaceOrderResult(orderId, false, "Payment failed: " + paymentResult.errorMessage());
        }
    }
}
