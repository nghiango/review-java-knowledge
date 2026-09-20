package lab.springcore.heavyinit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private final RemoteRateClient rateClient;
    private final Map<String, Double> rates = new ConcurrentHashMap<>();

    public ExchangeRateService(RemoteRateClient rateClient) {
        this.rateClient = Objects.requireNonNull(rateClient, "rateClient must not be null");
        // Safe static defaults loaded without remote I/O
        rates.put("USD", 1.0);
        rates.put("EUR", 1.05);
    }

    public void refreshRates() {
        try {
            Map<String, Double> fetched = rateClient.fetchRates();
            if (fetched != null) {
                rates.putAll(fetched);
            }
        } catch (Exception e) {
            // Graceful fallback to existing cached rates
        }
    }

    public double getRate(String currency) {
        return rates.getOrDefault(currency, 1.0);
    }
}
