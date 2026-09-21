package lab.designpatterns.decorator;

public interface OrderQueryPort {
    OrderSummary getOrderSummary(String orderId);
}
