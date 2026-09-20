# Solution — Chain Without Error Handling

## Annotated code

```java
package lab.webflux.broken.noerrorhandler;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CartPricingEngine {

  private final WebClient loyaltyClient;
  private final WebClient couponClient;

  public CartPricingEngine(WebClient.Builder builder) {
    this.loyaltyClient = builder.baseUrl("https://loyalty.internal").build();
    this.couponClient = builder.baseUrl("https://coupons.internal").build();
  }

  public Mono<FinalCartPrice> calculateCartPrice(String userId, double baseTotal) {
    // Resilience issue: Missing .onErrorResume() or .onErrorReturn() on non-critical loyalty discount branch.
    // In Reactive Streams, an onError signal is terminal. If the loyalty service returns 500, times out,
    // or is unreachable, the error signal aborts the entire stream immediately.
    Mono<Double> loyaltyDiscountMono = loyaltyClient.get()
        .uri("/loyalty/{user}/discount", userId)
        .retrieve()
        .bodyToMono(LoyaltyDiscount.class)
        .map(LoyaltyDiscount::discountAmount);

    // Reliability issue: Mono.zip terminates immediately if EITHER publisher emits an error signal.
    // If the coupon service has a transient 503 error, Mono.zip drops the loyalty discount, fails the cart calculation,
    // and returns HTTP 500 to the checkout page, preventing the customer from buying products.
    Mono<Double> couponDiscountMono = couponClient.get()
        .uri("/coupons/{user}/best", userId)
        .retrieve()
        .bodyToMono(CouponDiscount.class)
        .map(CouponDiscount::couponValue);

    // Combining discounts using zip without error isolation
    return Mono.zip(loyaltyDiscountMono, couponDiscountMono)
        .map(tuple -> {
          double loyalty = tuple.getT1();
          double coupon = tuple.getT2();
          double totalDiscount = loyalty + coupon;
          return new FinalCartPrice(userId, baseTotal, totalDiscount, Math.max(0, baseTotal - totalDiscount));
        });
  }

  public record LoyaltyDiscount(String userId, double discountAmount) {}
  public record CouponDiscount(String userId, double couponValue) {}
  public record FinalCartPrice(String userId, double originalTotal, double discount, double finalTotal) {}
}
```

## Issue list

### Resilience issue: Terminal `onError` in `Mono.zip` collapses entire checkout calculation

- **Location:** `CartPricingEngine.java:23`
- **Description:** Non-critical marketing/promotional service calls lack error isolation handlers (`.onErrorReturn(0.0)`).
- **Impact:** If the coupon service experiences a transient network error, `Mono.zip` immediately terminates with an error. The user cannot checkout their cart, converting an optional promotional failure into a catastrophic revenue loss.
- **Remediation:** Decorate each independent non-critical publisher with `.onErrorReturn(0.0)` or `.onErrorResume(...)` so the stream continues with fallback default values.

### Reliability issue: Missing timeout protection on combined parallel publishers

- **Location:** `CartPricingEngine.java:31`
- **Description:** Neither branch defines an execution timeout, causing `Mono.zip` to wait as long as the slowest publisher.
- **Impact:** If one dependency hangs, the entire user request hangs.
- **Remediation:** Attach explicit `.timeout(Duration)` to each branch before combining.

## Correct implementation

See [`lab.webflux.noerrorhandler.CorrectCartPricingEngine`](../../src/main/java/lab/webflux/noerrorhandler/CorrectCartPricingEngine.java).

Detailed discussion in [Solutions](../../../docs/topics/webclient-webflux/solutions.md#reactive-error-handling-and-graceful-degradation).
