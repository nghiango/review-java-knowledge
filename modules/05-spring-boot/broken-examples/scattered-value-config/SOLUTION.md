# Solution: Scattered Value Configuration

## Annotated Code

### `BillingConfigConsumer.java`

```java
package lab.springboot.broken.propsbinding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingConfigConsumer {

    // Maintainability issue: Scattered raw property key lacks hierarchical grouping and IDE autocomplete support
    // Validation issue: No constraint validation (@Positive, @Min) allows negative or zero rate without fail-fast on startup
    @Value("${app.billing.rate:0.0}")
    private double billingRate;

    // Maintainability issue: Raw String without ISO currency validation (@Pattern or Currency type)
    @Value("${app.billing.currency:USD}")
    private String currency;

    // Validation issue: Raw integer without bounds check (@Min(0), @Max(100)) allows invalid tax percentages
    @Value("${app.billing.tax-percent:0}")
    private int taxPercent;

    public double calculateCharge(double baseAmount) {
        return baseAmount * (1.0 + (taxPercent / 100.0)) * billingRate;
    }

    public double getBillingRate() {
        return billingRate;
    }

    public String getCurrency() {
        return currency;
    }

    public int getTaxPercent() {
        return taxPercent;
    }
}
```

### `PaymentNotificationService.java`

```java
package lab.springboot.broken.propsbinding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationService {

    // Code smell issue: Duplicate default fallback "USD" hardcoded across multiple unrelated classes
    @Value("${app.billing.currency:USD}")
    private String currency;

    // Maintainability issue: Duplicate property key definition without central single source of truth
    @Value("${app.billing.rate}")
    private double billingRate;

    public String formatNotification(double amount) {
        return "Charge: " + (amount * billingRate) + " " + currency;
    }

    public String getCurrency() {
        return currency;
    }

    public double getBillingRate() {
        return billingRate;
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `BillingConfigConsumer.java:11` | `Maintainability` | Scattered unvalidated property key | Raw `@Value` strings are fragile, lack refactoring support, and decentralize configuration definitions. |
| `BillingConfigConsumer.java:11` | `Validation` | Missing `@Positive` validation | An invalid or negative billing rate silently enters the system without failing fast at bootstrap. |
| `BillingConfigConsumer.java:19` | `Validation` | Missing bounds check on tax percentage | Allows nonsensical tax percentages (e.g. -50% or 1000%) without constraint checks. |
| `PaymentNotificationService.java:10` | `Code smell` | Duplicated property keys and fallback defaults | If the default currency changes, developers must locate and update every `@Value` occurrence. |

## Correct implementation

- Package: `lab.springboot.propsbinding`
- Production reference: `BillingProperties.java`, `BillingConfigConsumer.java`, `PaymentNotificationService.java`
- Fix: Encapsulate configuration in a hierarchical, immutable `@ConfigurationProperties(prefix = "app.billing")` record annotated with `@Validated` and Bean Validation constraints (`@NotNull`, `@Positive`, `@Min(0)`, `@Max(100)`). Inject the strongly typed properties object via constructor injection.
