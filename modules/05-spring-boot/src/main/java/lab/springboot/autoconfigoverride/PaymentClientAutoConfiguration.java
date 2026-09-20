package lab.springboot.autoconfigoverride;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PaymentClientProperties.class)
public class PaymentClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExternalPaymentClient externalPaymentClient(PaymentClientProperties properties) {
        return new ExternalPaymentClient(
                properties.baseUrl(), properties.enableMetrics(), properties.enableTracing());
    }
}
