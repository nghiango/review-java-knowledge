package lab.springmvc.controllerseparation;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final PricingService pricingService;
    private final PaymentService paymentService;

    public CheckoutService(PricingService pricingService, PaymentService paymentService) {
        this.pricingService = pricingService;
        this.paymentService = paymentService;
    }

    public OrderResponse checkout(OrderRequest request) {
        double total = pricingService.calculateTotal(request.quantity(), request.unitPrice());
        boolean paymentSuccess =
                paymentService.processPayment(request.customerId(), total, request.paymentMethod());

        if (!paymentSuccess) {
            throw new IllegalStateException("Payment processing failed");
        }

        String orderId = UUID.randomUUID().toString();
        return new OrderResponse(orderId, request.customerId(), total, "CONFIRMED");
    }
}
