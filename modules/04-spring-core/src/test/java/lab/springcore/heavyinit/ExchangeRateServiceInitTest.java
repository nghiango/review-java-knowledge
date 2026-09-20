package lab.springcore.heavyinit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ExchangeRateServiceInitTest {

    @Test
    @DisplayName(
            "ExchangeRateService starts instantly with safe defaults and warms up via event listener")
    void contextRefreshed_warmsUpRates() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(
                    RemoteRateClient.class,
                    ExchangeRateService.class,
                    ExchangeRateWarmupListener.class);
            context.refresh();

            ExchangeRateService service = context.getBean(ExchangeRateService.class);

            assertThat(service.getRate("EUR")).isEqualTo(1.08);
            assertThat(service.getRate("GBP")).isEqualTo(1.27);
            assertThat(service.getRate("USD")).isEqualTo(1.0);
        }
    }
}
