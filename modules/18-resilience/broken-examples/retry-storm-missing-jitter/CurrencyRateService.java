package lab.resilience.broken.nojitter;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CurrencyRateService {

    private final ExternalForexProvider forexProvider;

    public CurrencyRateService(ExternalForexProvider forexProvider) {
        this.forexProvider = forexProvider;
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        int maxRetries = 5;
        long fixedBackoffMs = 500L;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return forexProvider.fetchRate(fromCurrency, toCurrency);
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Forex provider unavailable after " + maxRetries + " attempts", ex);
                }
                try {
                    Thread.sleep(fixedBackoffMs * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable state in getExchangeRate");
    }

    public interface ExternalForexProvider {
        BigDecimal fetchRate(String from, String to);
    }
}
