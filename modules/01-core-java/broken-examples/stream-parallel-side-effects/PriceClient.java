package lab.corejava.broken.streamprocessing;

import java.math.BigDecimal;

public interface PriceClient {
    BigDecimal lookup(String orderId);
}
