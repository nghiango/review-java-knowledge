package lab.corejava.streamprocessing;

@FunctionalInterface
public interface PriceClient {
    OrderPrice lookup(String orderId);
}
