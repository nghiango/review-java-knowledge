package lab.springmvc.controllerseparation;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public boolean processPayment(String customerId, double amount, String paymentMethod) {
        return amount > 0 && paymentMethod != null && !paymentMethod.isBlank();
    }
}
