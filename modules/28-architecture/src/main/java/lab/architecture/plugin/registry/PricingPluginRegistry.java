package lab.architecture.plugin.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lab.architecture.plugin.api.PricingRulePlugin;

/**
 * Extension registry holding discovered or configured plugins. Sorts plugins by their defined order
 * priority.
 */
public class PricingPluginRegistry {

    private final List<PricingRulePlugin> plugins = new ArrayList<>();

    public PricingPluginRegistry() {}

    public PricingPluginRegistry(List<PricingRulePlugin> initialPlugins) {
        if (initialPlugins != null) {
            for (PricingRulePlugin plugin : initialPlugins) {
                register(plugin);
            }
        }
    }

    public synchronized void register(PricingRulePlugin plugin) {
        Objects.requireNonNull(plugin, "Plugin must not be null");
        plugins.add(plugin);
        plugins.sort(Comparator.comparingInt(PricingRulePlugin::getOrder));
    }

    public synchronized List<PricingRulePlugin> getPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(plugins));
    }
}
