package lab.architecture.broken.domaininfrastructure;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    public boolean chargeCreditCard(String customerEmail, BigDecimal amount) {
        // Simulates outbound HTTP call to payment gateway
        return amount.compareTo(new BigDecimal("10000.00")) < 0;
    }
}
