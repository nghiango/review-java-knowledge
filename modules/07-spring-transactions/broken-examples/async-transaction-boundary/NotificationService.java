package lab.springtransactions.broken.asynctransaction;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Async
    public void sendOrderConfirmation(String orderId) {
        // Query order from database to send email
        // If the calling transaction has not committed yet, this async thread reads stale/uncommitted data or fails with not found!
    }
}
