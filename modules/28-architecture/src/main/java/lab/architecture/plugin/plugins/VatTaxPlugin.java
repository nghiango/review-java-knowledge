package lab.architecture.plugin.plugins;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lab.architecture.plugin.api.PricingRulePlugin;
import lab.architecture.plugin.core.PricingContext;

/** Plugin that applies EU VAT (20%) on prices. */
public class VatTaxPlugin implements PricingRulePlugin {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");

    @Override
    public String getPluginId() {
        return "eu-vat-tax-plugin";
    }

    @Override
    public int getOrder() {
        return 100; // Evaluated after discounts
    }

    @Override
    public boolean supports(PricingContext context) {
        return "EU".equalsIgnoreCase(context.getRegion());
    }

    @Override
    public void apply(PricingContext context) {
        BigDecimal tax =
                context.getFinalPrice().multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        context.setFinalPrice(context.getFinalPrice().add(tax));
        context.addAdjustment("Applied EU VAT (20%): +" + tax);
    }
}
