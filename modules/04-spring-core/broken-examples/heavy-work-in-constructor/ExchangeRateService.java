package lab.springcore.broken.heavyinit;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private final RemoteRateClient rateClient;
    private final Map<String, Double> rates = new HashMap<>();

    public ExchangeRateService(RemoteRateClient rateClient) {
        this.rateClient = rateClient;
    }

    @PostConstruct
    public void init() {
        // Anti-pattern: Blocking network call inside @PostConstruct stalls Spring startup
        rates.putAll(rateClient.fetchRates());

        // Anti-pattern: Spawning unmanaged background thread with infinite loop inside bean init
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    rates.putAll(rateClient.fetchRates());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public double getRate(String currency) {
        return rates.getOrDefault(currency, 1.0);
    }
}
