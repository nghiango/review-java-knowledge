package lab.springmvc.controllerseparation;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public double calculateTotal(int quantity, double unitPrice) {
        double subtotal = quantity * unitPrice;
        double discount = 0.0;
        if (quantity > 10) {
            discount = subtotal * 0.15;
        }
        return subtotal - discount;
    }
}
