package lab.springboot.broken.autoconfigoverride;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomRestClientConfig {

    @Bean
    public ExternalPaymentClient externalPaymentClient() {
        return new ExternalPaymentClient("https://custom-payments.example.com", false, false);
    }
}
