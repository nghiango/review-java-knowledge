package lab.springboot.propsbinding;

import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationService {

    private final BillingProperties properties;

    public PaymentNotificationService(BillingProperties properties) {
        this.properties = properties;
    }

    public String formatNotification(double amount) {
        return "Charge: " + (amount * properties.rate()) + " " + properties.currency();
    }

    public BillingProperties getProperties() {
        return properties;
    }
}
