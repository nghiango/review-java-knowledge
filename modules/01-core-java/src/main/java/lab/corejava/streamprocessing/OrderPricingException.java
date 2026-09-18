package lab.corejava.streamprocessing;

public final class OrderPricingException extends RuntimeException {
    private final String orderId;

    public OrderPricingException(String orderId, Throwable cause) {
        super("Failed to price order: " + orderId, cause);
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
