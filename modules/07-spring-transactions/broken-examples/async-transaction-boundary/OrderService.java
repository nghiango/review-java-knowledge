package lab.springtransactions.broken.asynctransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional
    public void createOrder(String orderId, double amount) {
        insertOrder(orderId, amount);

        // Invoking @Async method from within @Transactional
        // The async method runs in a separate thread where the calling transaction is NOT visible (ThreadLocal not inherited),
        // and if this transaction later rolls back, the notification has ALREADY been sent!
        notificationService.sendOrderConfirmation(orderId);

        if (amount > 10000) {
            throw new IllegalArgumentException("Amount exceeds credit limit - rolling back");
        }
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }
}
