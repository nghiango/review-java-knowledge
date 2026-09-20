# Solution: Accidental Auto-Configuration Override

## Annotated Code

### `CustomRestClientConfig.java`

```java
package lab.springboot.broken.autoconfigoverride;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomRestClientConfig {

    // Maintainability issue: Constructing a raw bean directly overrides framework auto-configuration defaults without applying customizers
    // Observability issue: Completely bypasses auto-configured Micrometer observation, tracing propagation, and client request metrics
    // Reliability issue: Lacks connection pool tuning, timeout settings, and SSL configuration provided by standard auto-configuration builders
    @Bean
    public ExternalPaymentClient externalPaymentClient() {
        return new ExternalPaymentClient("https://custom-payments.example.com", false, false);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `CustomRestClientConfig.java:13` | `Maintainability` | Raw bean instantiation bypassing auto-configuration | Defining a raw client bean prevents Spring Boot's auto-configured builders and customizer chains from running. |
| `CustomRestClientConfig.java:13` | `Observability` | Lost metrics and tracing instrumentation | Production telemetry relies on framework auto-configuration; bypassing it loses distributed tracing headers and latency metrics. |
| `CustomRestClientConfig.java:13` | `Reliability` | Missing connection pooling and timeout customization | Raw clients without standardized builder configuration risk thread exhaustion and unmonitored socket hangs. |

## Correct implementation

- Package: `lab.springboot.autoconfigoverride`
- Production reference: `ExternalPaymentClient.java`, `PaymentClientProperties.java`, `PaymentClientAutoConfiguration.java`, `PaymentProcessingService.java`
- Fix: Leverage Spring Boot's builder/customizer pattern (e.g. `ClientCustomizer`) and guard auto-configured bean definitions with `@ConditionalOnMissingBean`. Customizations should be applied to builders rather than replacing auto-configured infrastructure with unmanaged instances.
