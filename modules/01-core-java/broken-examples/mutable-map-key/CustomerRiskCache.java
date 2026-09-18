package lab.corejava.broken.mutablemapkey;

import java.util.HashMap;
import java.util.Map;

public class CustomerRiskCache {
    private final Map<CustomerKey, String> riskByCustomer = new HashMap<>();

    public void remember(CustomerKey key, String riskLevel) {
        riskByCustomer.put(key, riskLevel);
    }

    public void refreshCustomerId(CustomerKey key, String canonicalCustomerId) {
        key.setCustomerId(canonicalCustomerId);
    }

    public String riskFor(CustomerKey key) {
        return riskByCustomer.get(key);
    }
}
