package lab.springboot.autoconfigoverride;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class PaymentClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PaymentClientAutoConfiguration.class));

    @Test
    @DisplayName("Auto-configuration should provide default ExternalPaymentClient bean")
    void autoConfiguration_providesDefaultClient() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ExternalPaymentClient.class);
                    ExternalPaymentClient client = context.getBean(ExternalPaymentClient.class);
                    assertThat(client.getBaseUrl())
                            .isEqualTo("https://api.payments.default.internal");
                });
    }

    @Test
    @DisplayName(
            "User-defined bean should back off auto-configured ExternalPaymentClient via @ConditionalOnMissingBean")
    void userDefinedBean_backsOffAutoConfiguration() {
        contextRunner
                .withUserConfiguration(CustomClientConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ExternalPaymentClient.class);
                            ExternalPaymentClient client =
                                    context.getBean(ExternalPaymentClient.class);
                            assertThat(client.getBaseUrl())
                                    .isEqualTo("https://custom.override.internal");
                        });
    }

    @Configuration
    static class CustomClientConfiguration {
        @Bean
        public ExternalPaymentClient customPaymentClient() {
            return new ExternalPaymentClient("https://custom.override.internal", true, true);
        }
    }
}
