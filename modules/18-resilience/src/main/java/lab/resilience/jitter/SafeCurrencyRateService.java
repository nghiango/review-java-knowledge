package lab.resilience.jitter;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class SafeCurrencyRateService {

    private final ExternalForexProvider forexProvider;
    private final Retry retry;

    public SafeCurrencyRateService(ExternalForexProvider forexProvider) {
        this(forexProvider, createDefaultRetry());
    }

    public SafeCurrencyRateService(ExternalForexProvider forexProvider, Retry retry) {
        this.forexProvider = forexProvider;
        this.retry = retry;
    }

    public static Retry createDefaultRetry() {
        IntervalFunction fullJitter =
                IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(100), 2.0, 0.6);

        RetryConfig config =
                RetryConfig.custom()
                        .maxAttempts(4)
                        .intervalFunction(fullJitter)
                        .retryExceptions(TransientForexException.class)
                        .build();

        return Retry.of("forexRetry", config);
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return Retry.decorateSupplier(
                        retry, () -> forexProvider.fetchRate(fromCurrency, toCurrency))
                .get();
    }

    public interface ExternalForexProvider {
        BigDecimal fetchRate(String from, String to);
    }

    public static class TransientForexException extends RuntimeException {
        public TransientForexException(String message) {
            super(message);
        }
    }
}
