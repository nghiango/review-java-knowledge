package lab.springtransactions.externalcall;

import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    public boolean chargeCard(String accountId, double amount) {
        // Simulates remote HTTP client call outside any database transaction
        return !"declined-account".equals(accountId);
    }
}
