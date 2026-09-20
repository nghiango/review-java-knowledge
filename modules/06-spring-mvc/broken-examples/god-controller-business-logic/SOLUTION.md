# Solution: God Controller with Business Logic

## Annotated Code

### `CheckoutController.java`

```java
package lab.springmvc.broken.controllerseparation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @PostMapping
    public String checkout(@RequestBody OrderRequest request) {
        // Validation issue: Ad-hoc manual validation inside controller instead of declarative Bean Validation
        if (request.quantity() <= 0) {
            return "Invalid quantity";
        }
        // Architecture issue: Business pricing algorithms directly inside controller prevents reuse and unit testability
        double subtotal = request.quantity() * request.unitPrice();
        double discount = 0.0;
        if (request.quantity() > 10) {
            discount = subtotal * 0.15;
        }
        double total = subtotal - discount;

        // Security issue: SQL string concatenation introduces SQL injection vulnerability
        // Architecture issue: Controller executing direct database queries violates layered architecture
        String sql =
                "INSERT INTO orders (customer_id, product_id, total) VALUES ('"
                        + request.customerId()
                        + "', '"
                        + request.productId()
                        + "', "
                        + total
                        + ")";

        // Architecture issue: External payment and email notification concerns coupled directly to web endpoint
        String paymentStatus = "SUCCESS";
        String notification = "Email sent to customer " + request.customerId();

        return "Order processed: total="
                + total
                + ", payment="
                + paymentStatus
                + ", sql="
                + sql.length()
                + ", notif="
                + notification;
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `CheckoutController.java:16` | `Validation` | Manual validation in web layer | Missing declarative `@Valid` and domain validation constraints. |
| `CheckoutController.java:20` | `Architecture` | Pricing logic embedded in controller | Violates Single Responsibility Principle; pricing rules cannot be unit tested without web layer. |
| `CheckoutController.java:28` | `Security` | SQL string concatenation | Vulnerable to SQL injection if input contains single quotes. |
| `CheckoutController.java:37` | `Architecture` | God controller anti-pattern | Couples HTTP handling, database I/O, payment integrations, and notifications in a single controller. |

## Correct implementation

- Package: `lab.springmvc.controllerseparation`
- Production reference: `PricingService.java`, `PaymentService.java`, `CheckoutService.java`, `CheckoutController.java`
- Fix: Decompose into layered architecture: Controller is a thin adapter translating HTTP to domain requests; `PricingService` handles discounts; `PaymentService` manages transactions; `CheckoutService` orchestrates the domain workflow.
