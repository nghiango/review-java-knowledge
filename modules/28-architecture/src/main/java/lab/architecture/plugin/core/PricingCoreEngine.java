package lab.architecture.plugin.core;

import java.math.BigDecimal;
import java.util.Objects;
import lab.architecture.plugin.api.PricingRulePlugin;
import lab.architecture.plugin.registry.PricingPluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Microkernel Core Engine. Minimal, stable core system that coordinates calculation lifecycle. The
 * core does not know about specific country taxes or business promotions; it delegates to external
 * plugins via the registry.
 */
public class PricingCoreEngine {

    private static final Logger log = LoggerFactory.getLogger(PricingCoreEngine.class);

    private final PricingPluginRegistry pluginRegistry;

    public PricingCoreEngine(PricingPluginRegistry pluginRegistry) {
        this.pluginRegistry =
                Objects.requireNonNull(pluginRegistry, "PricingPluginRegistry must not be null");
    }

    public PricingContext calculate(String customerId, String region, BigDecimal basePrice) {
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }

        PricingContext context = new PricingContext(customerId, region, basePrice);
        log.info(
                "Starting calculation pipeline for base price [{}] in region [{}]",
                basePrice,
                region);

        for (PricingRulePlugin plugin : pluginRegistry.getPlugins()) {
            if (plugin.supports(context)) {
                log.debug(
                        "Executing plugin [{}] (order: {})",
                        plugin.getPluginId(),
                        plugin.getOrder());
                plugin.apply(context);
            }
        }

        log.info(
                "Calculation pipeline completed. Final price: [{}] with {} adjustments",
                context.getFinalPrice(),
                context.getAppliedAdjustments().size());
        return context;
    }
}
