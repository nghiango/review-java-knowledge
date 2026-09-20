package lab.springcore.broken.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private BillingService billingService;

    public void createOrder(String orderId, double amount) {
        // Business logic
        billingService.processInvoice(orderId, amount);
    }

    public void markOrderPaid(String orderId) {
        // Update order status to paid
    }
}
