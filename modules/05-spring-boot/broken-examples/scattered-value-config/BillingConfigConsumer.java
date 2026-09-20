package lab.springboot.broken.propsbinding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingConfigConsumer {

    @Value("${app.billing.rate:0.0}")
    private double billingRate;

    @Value("${app.billing.currency:USD}")
    private String currency;

    @Value("${app.billing.tax-percent:0}")
    private int taxPercent;

    public double calculateCharge(double baseAmount) {
        return baseAmount * (1.0 + (taxPercent / 100.0)) * billingRate;
    }

    public double getBillingRate() {
        return billingRate;
    }

    public String getCurrency() {
        return currency;
    }

    public int getTaxPercent() {
        return taxPercent;
    }
}
