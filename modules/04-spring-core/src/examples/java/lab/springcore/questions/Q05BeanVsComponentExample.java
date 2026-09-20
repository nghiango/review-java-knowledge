package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Q05: Demonstrates @Bean vs @Component. */
@SuppressWarnings("unused")
public class Q05BeanVsComponentExample {

    static class ThirdPartyClient {
        private final String endpoint;

        ThirdPartyClient(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    @Configuration
    static class AppConfig {
        // @Bean is required for third-party classes outside codebase control
        @Bean
        ThirdPartyClient thirdPartyClient() {
            return new ThirdPartyClient("https://api.external.com");
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class)) {
            ThirdPartyClient client = context.getBean(ThirdPartyClient.class);
            String url = client.getEndpoint(); // "https://api.external.com"
        }
    }
}
