package lab.architecture.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import lab.architecture.plugin.core.PricingContext;
import lab.architecture.plugin.core.PricingCoreEngine;
import lab.architecture.plugin.plugins.LoyaltyDiscountPlugin;
import lab.architecture.plugin.plugins.VatTaxPlugin;
import lab.architecture.plugin.registry.PricingPluginRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PricingPluginEngineTest {

    @Test
    @DisplayName("Microkernel applies registered plugins in defined order")
    void calculate_appliesPluginsInOrder() {
        PricingPluginRegistry registry = new PricingPluginRegistry();
        registry.register(new VatTaxPlugin()); // order = 100
        registry.register(new LoyaltyDiscountPlugin()); // order = 10

        PricingCoreEngine engine = new PricingCoreEngine(registry);

        // Base price: $100.00
        // VIP customer triggers discount first (-$10.00 => $90.00)
        // EU region triggers VAT (+20% of $90.00 = +$18.00 => $108.00)
        PricingContext result = engine.calculate("VIP-12345", "EU", new BigDecimal("100.00"));

        assertThat(result.getFinalPrice()).isEqualByComparingTo("108.00");
        assertThat(result.getAppliedAdjustments()).hasSize(2);
        assertThat(result.getAppliedAdjustments().get(0)).contains("VIP Loyalty Discount");
        assertThat(result.getAppliedAdjustments().get(1)).contains("Applied EU VAT");
    }

    @Test
    @DisplayName("Microkernel handles non-applicable plugins gracefully without altering price")
    void calculate_nonMatchingContext_leavesBasePriceUnchanged() {
        PricingPluginRegistry registry = new PricingPluginRegistry();
        registry.register(new VatTaxPlugin());
        registry.register(new LoyaltyDiscountPlugin());

        PricingCoreEngine engine = new PricingCoreEngine(registry);

        // Regular customer in US region: Neither plugin should support this context
        PricingContext result = engine.calculate("REGULAR-999", "US", new BigDecimal("50.00"));

        assertThat(result.getFinalPrice()).isEqualByComparingTo("50.00");
        assertThat(result.getAppliedAdjustments()).isEmpty();
    }
}
