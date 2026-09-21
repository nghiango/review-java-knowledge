# Solution: Procedural Switch-on-Type Growth

## Annotated code

```java
package lab.designpatterns.broken.switchontype;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    // Maintainability issue: Open/Closed Principle violation; every new payment type requires editing this monolithic method
    // Testing issue: Monolithic switch prevents isolated unit testing of specific payment rails without mocking unrelated dependencies
    public boolean processPayment(PaymentRequest request) {
        log.info("Processing payment for transaction {}", request.transactionId());

        switch (request.paymentType()) {
            case CREDIT_CARD:
                if (request.paymentDetails() == null || request.paymentDetails().length() < 16) {
                    throw new IllegalArgumentException("Invalid credit card format");
                }
                log.info("Contacting Visa/MasterCard gateway for {}", request.amount());
                return true;

            case DEBIT_CARD:
                if (!request.paymentDetails().startsWith("DEBIT-")) {
                    throw new IllegalArgumentException("Invalid debit card format");
                }
                log.info("Contacting Interac/Direct Debit network for {}", request.amount());
                return true;

            case PAYPAL:
                if (!request.paymentDetails().contains("@")) {
                    throw new IllegalArgumentException("Invalid PayPal email address");
                }
                log.info("Creating PayPal checkout session for {}", request.amount());
                return true;

            case CRYPTO:
                if (!request.paymentDetails().startsWith("0x")) {
                    throw new IllegalArgumentException("Invalid wallet address");
                }
                log.info("Checking blockchain confirmations for {}", request.amount());
                return true;

            case BANK_TRANSFER:
                if (request.paymentDetails().length() != 10) {
                    throw new IllegalArgumentException("Invalid IBAN length");
                }
                log.info("Initiating SEPA transfer for {}", request.amount());
                return true;

            default:
                // Maintainability issue: Missing compile-time exhaustiveness check; adding enum value fails at runtime
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }

    // Maintainability issue: Duplicated switch logic; every new rail must be synchronized across processPayment, calculateFee, and refund
    public BigDecimal calculateFee(PaymentRequest request) {
        switch (request.paymentType()) {
            case CREDIT_CARD:
                return request.amount().multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
            case DEBIT_CARD:
                return new BigDecimal("0.25");
            case PAYPAL:
                return request.amount().multiply(new BigDecimal("0.034")).add(new BigDecimal("0.35"));
            case CRYPTO:
                return new BigDecimal("0.0005");
            case BANK_TRANSFER:
                return BigDecimal.ZERO;
            default:
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }

    // Maintainability issue: Triplicated switch statement increasing cyclomatic complexity and regression risk
    public void refund(PaymentRequest request) {
        switch (request.paymentType()) {
            case CREDIT_CARD:
                log.info("Issuing credit card chargeback reversal");
                break;
            case DEBIT_CARD:
                log.info("Reversing direct debit entry");
                break;
            case PAYPAL:
                log.info("Triggering PayPal refund API");
                break;
            case CRYPTO:
                throw new UnsupportedOperationException("Crypto payments cannot be automatically refunded");
            case BANK_TRANSFER:
                log.info("Queueing wire transfer return");
                break;
            default:
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }
}
```

## Issue analysis

### 1. Violation of the Open/Closed Principle (Maintainability)
- **Problem**: Whenever product managers add a new payment option (such as `APPLE_PAY`, `KLARNA`, or `PIX`), developers must open `PaymentProcessor.java` and modify three separate switch statements (`processPayment`, `calculateFee`, and `refund`).
- **Consequence**: High merge conflict rate in multi-developer teams, high regression risk where an edit to Apple Pay accidentally breaks Credit Card processing, and explosive class growth.

### 2. High Cyclomatic Complexity & Untestable Monolith (Testing)
- **Problem**: All third-party dependencies, validation rules, and network communication are entangled within one class.
- **Consequence**: A single test for PayPal requires instantiating the entire processor. Writing exhaustive test matrices requires factorial branch permutations.

---

## Correct implementation

The production solution uses the **Strategy Pattern** paired with a Spring-managed **Factory / Registry**:
- Package: `lab.designpatterns.strategy`
- Define a cohesive `PaymentStrategy` interface:
  ```java
  public interface PaymentStrategy {
      PaymentType getSupportedType();
      boolean processPayment(PaymentRequest request);
      BigDecimal calculateFee(PaymentRequest request);
      void refund(PaymentRequest request);
  }
  ```
- Each payment rail becomes an independent, isolated Spring `@Component`:
  - `CreditCardPaymentStrategy`
  - `PayPalPaymentStrategy`
- A `PaymentStrategyFactory` auto-collects all strategies via Spring constructor injection (`List<PaymentStrategy>`) and maps them into an immutable `Map<PaymentType, PaymentStrategy>`.
- Adding `APPLE_PAY` requires writing exactly one new class implementing `PaymentStrategy` with zero edits to existing classes!

---

## Trade-offs

| Dimension | Switch-on-Type (Procedural) | Strategy + Factory (OOP) |
|---|---|---|
| **Class Count** | 1 single class | $N+2$ classes (interface + registry + strategies) |
| **Open/Closed** | Fails: modified on every new type | Adheres: new features added via new files |
| **Testing** | Difficult: massive mocks required | Easy: each strategy tested in total isolation |
| **Spring Integration** | Manual wiring | Dynamic discovery via `@Component` |
