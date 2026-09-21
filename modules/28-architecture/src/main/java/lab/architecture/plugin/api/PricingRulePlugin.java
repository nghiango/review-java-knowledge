package lab.architecture.plugin.api;

import lab.architecture.plugin.core.PricingContext;

/**
 * Extension SPI (Service Provider Interface) for pricing rules. Plugins can be contributed
 * independently without modifying the core microkernel engine.
 */
public interface PricingRulePlugin {

    /** Unique identifier of the plugin. */
    String getPluginId();

    /** Execution order / priority. Lower values run earlier in the calculation pipeline. */
    int getOrder();

    /** Determines whether this plugin applies to the current context. */
    boolean supports(PricingContext context);

    /** Applies this pricing transformation rule to the context. */
    void apply(PricingContext context);
}
