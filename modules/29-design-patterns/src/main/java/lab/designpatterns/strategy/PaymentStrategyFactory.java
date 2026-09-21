package lab.designpatterns.strategy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Spring-managed Factory/Registry mapping PaymentType to its concrete strategy. Strategies are
 * discovered automatically via Spring's constructor injection of List<PaymentStrategy>.
 */
@Component
public class PaymentStrategyFactory {

    private final Map<PaymentType, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        Objects.requireNonNull(strategyList, "Strategy list must not be null");
        Map<PaymentType, PaymentStrategy> map = new EnumMap<>(PaymentType.class);
        for (PaymentStrategy strategy : strategyList) {
            map.put(strategy.getSupportedType(), strategy);
        }
        this.strategies = Collections.unmodifiableMap(map);
    }

    public PaymentStrategy getStrategy(PaymentType type) {
        PaymentStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new UnsupportedOperationException(
                    "No payment strategy registered for type: " + type);
        }
        return strategy;
    }

    public boolean hasStrategy(PaymentType type) {
        return strategies.containsKey(type);
    }
}
