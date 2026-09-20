package lab.springcore.broken.heavyinit;

import java.util.Map;

public class RemoteRateClient {
    public Map<String, Double> fetchRates() {
        try {
            // Simulated slow remote API call
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Map.of("EUR", 1.08, "GBP", 1.27);
    }
}
