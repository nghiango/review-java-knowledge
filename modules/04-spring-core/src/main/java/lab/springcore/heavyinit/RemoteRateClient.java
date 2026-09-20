package lab.springcore.heavyinit;

import java.util.Map;

public class RemoteRateClient {
    public Map<String, Double> fetchRates() {
        return Map.of("EUR", 1.08, "GBP", 1.27);
    }
}
