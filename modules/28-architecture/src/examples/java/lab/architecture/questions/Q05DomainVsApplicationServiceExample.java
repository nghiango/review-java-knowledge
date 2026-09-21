package lab.architecture.questions;

import java.math.BigDecimal;

/**
 * Q05: Domain Service vs Application Service. Demonstrates the boundary: Domain Service holds
 * domain logic involving multiple models, while Application Service orchestrates transactions,
 * security, and I/O.
 */
public class Q05DomainVsApplicationServiceExample {

    // Domain Service: Pure business calculation spanning two concepts without I/O
    public static class CurrencyExchangeDomainService {
        public BigDecimal convert(BigDecimal amount, BigDecimal exchangeRate) {
            if (amount.compareTo(BigDecimal.ZERO) < 0
                    || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid conversion inputs");
            }
            return amount.multiply(exchangeRate);
        }
    }

    // Application Service: Orchestration, transaction coordination, infrastructure calls
    public static class PaymentApplicationService {
        private final CurrencyExchangeDomainService exchangeService =
                new CurrencyExchangeDomainService();

        public BigDecimal executePayment(BigDecimal usdAmount, BigDecimal eurRate) {
            // Orchestrates: auth check, fetch from DB, call domain service, save
            BigDecimal eurAmount = exchangeService.convert(usdAmount, eurRate);
            return eurAmount;
        }
    }

    public static void main(String[] args) {
        CurrencyExchangeDomainService domainService = new CurrencyExchangeDomainService();
        BigDecimal converted =
                domainService.convert(new BigDecimal("100.00"), new BigDecimal("0.90")); // 90.0000

        boolean valid = converted.compareTo(new BigDecimal("90.00")) == 0; // true
        System.out.println("Q05 converted: " + converted + ", valid: " + valid);
    }
}
