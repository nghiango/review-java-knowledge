package lab.springcore.broken.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    @Autowired
    private OrderService orderService;

    public void processInvoice(String orderId, double amount) {
        // Charge customer
        orderService.markOrderPaid(orderId);
    }
}
