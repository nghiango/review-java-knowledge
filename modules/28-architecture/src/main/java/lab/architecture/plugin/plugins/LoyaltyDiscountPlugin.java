package lab.architecture.plugin.plugins;

import java.math.BigDecimal;
import lab.architecture.plugin.api.PricingRulePlugin;
import lab.architecture.plugin.core.PricingContext;

/** Plugin that provides a $10 flat discount for VIP loyalty customers. */
public class LoyaltyDiscountPlugin implements PricingRulePlugin {

    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("10.00");

    @Override
    public String getPluginId() {
        return "vip-loyalty-discount-plugin";
    }

    @Override
    public int getOrder() {
        return 10; // Evaluated early before taxes
    }

    @Override
    public boolean supports(PricingContext context) {
        return context.getCustomerId().startsWith("VIP-");
    }

    @Override
    public void apply(PricingContext context) {
        BigDecimal discounted = context.getFinalPrice().subtract(VIP_DISCOUNT);
        if (discounted.compareTo(BigDecimal.ZERO) < 0) {
            discounted = BigDecimal.ZERO;
        }
        context.setFinalPrice(discounted);
        context.addAdjustment("Applied VIP Loyalty Discount: -$10.00");
    }
}
