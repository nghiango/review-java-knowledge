package lab.springtransactions.broken.remoteapi;

import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    public boolean chargeCard(String accountId, double amount) {
        // Simulates remote HTTP REST call with potential 2-5s latency
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
