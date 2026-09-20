package lab.springboot.propsbinding;

import org.springframework.stereotype.Service;

@Service
public class BillingConfigConsumer {

    private final BillingProperties properties;

    public BillingConfigConsumer(BillingProperties properties) {
        this.properties = properties;
    }

    public double calculateCharge(double baseAmount) {
        return baseAmount * (1.0 + (properties.taxPercent() / 100.0)) * properties.rate();
    }

    public BillingProperties getProperties() {
        return properties;
    }
}
