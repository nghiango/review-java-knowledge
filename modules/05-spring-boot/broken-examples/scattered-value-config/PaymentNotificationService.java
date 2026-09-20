package lab.springboot.broken.propsbinding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationService {

    @Value("${app.billing.currency:USD}")
    private String currency;

    @Value("${app.billing.rate}")
    private double billingRate;

    public String formatNotification(double amount) {
        return "Charge: " + (amount * billingRate) + " " + currency;
    }

    public String getCurrency() {
        return currency;
    }

    public double getBillingRate() {
        return billingRate;
    }
}
