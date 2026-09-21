package lab.architecture.plugin.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Mutable calculation context passed through the Microkernel plugin pipeline. */
public class PricingContext {

    private final String customerId;
    private final String region;
    private final BigDecimal basePrice;
    private BigDecimal finalPrice;
    private final List<String> appliedAdjustments = new ArrayList<>();

    public PricingContext(String customerId, String region, BigDecimal basePrice) {
        this.customerId = Objects.requireNonNull(customerId);
        this.region = Objects.requireNonNull(region);
        this.basePrice = Objects.requireNonNull(basePrice);
        this.finalPrice = basePrice;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRegion() {
        return region;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = Objects.requireNonNull(finalPrice);
    }

    public void addAdjustment(String adjustmentDescription) {
        this.appliedAdjustments.add(Objects.requireNonNull(adjustmentDescription));
    }

    public List<String> getAppliedAdjustments() {
        return Collections.unmodifiableList(appliedAdjustments);
    }
}
