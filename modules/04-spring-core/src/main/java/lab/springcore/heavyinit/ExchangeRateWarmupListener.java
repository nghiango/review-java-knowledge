package lab.springcore.heavyinit;

import java.util.Objects;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateWarmupListener {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateWarmupListener(ExchangeRateService exchangeRateService) {
        this.exchangeRateService =
                Objects.requireNonNull(exchangeRateService, "exchangeRateService must not be null");
    }

    @EventListener
    public void onApplicationRefreshed(ContextRefreshedEvent event) {
        // Asynchronous or non-blocking warmup executed after the full context is active
        exchangeRateService.refreshRates();
    }
}
