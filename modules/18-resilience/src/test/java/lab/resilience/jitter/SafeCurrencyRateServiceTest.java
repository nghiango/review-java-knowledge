package lab.resilience.jitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SafeCurrencyRateServiceTest {

    @Test
    @DisplayName("Should retrieve rate on first attempt when provider is healthy")
    void getExchangeRate_healthyProvider() {
        SafeCurrencyRateService.ExternalForexProvider provider =
                mock(SafeCurrencyRateService.ExternalForexProvider.class);
        when(provider.fetchRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));

        SafeCurrencyRateService service = new SafeCurrencyRateService(provider);
        BigDecimal rate = service.getExchangeRate("USD", "EUR");

        assertThat(rate).isEqualTo(new BigDecimal("0.92"));
        verify(provider, times(1)).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should retry with jitter and succeed when provider recovers")
    void getExchangeRate_recoversWithJitter() {
        SafeCurrencyRateService.ExternalForexProvider provider =
                mock(SafeCurrencyRateService.ExternalForexProvider.class);
        when(provider.fetchRate("USD", "GBP"))
                .thenThrow(new SafeCurrencyRateService.TransientForexException("Forex timeout"))
                .thenReturn(new BigDecimal("0.78"));

        SafeCurrencyRateService service = new SafeCurrencyRateService(provider);
        BigDecimal rate = service.getExchangeRate("USD", "GBP");

        assertThat(rate).isEqualTo(new BigDecimal("0.78"));
        verify(provider, times(2)).fetchRate("USD", "GBP");
    }
}
