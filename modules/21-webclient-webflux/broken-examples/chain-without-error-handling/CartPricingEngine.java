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
    Mono<Double> loyaltyDiscountMono = loyaltyClient.get()
        .uri("/loyalty/{user}/discount", userId)
        .retrieve()
        .bodyToMono(LoyaltyDiscount.class)
        .map(LoyaltyDiscount::discountAmount);

    Mono<Double> couponDiscountMono = couponClient.get()
        .uri("/coupons/{user}/best", userId)
        .retrieve()
        .bodyToMono(CouponDiscount.class)
        .map(CouponDiscount::couponValue);

    // Combining discounts using zip
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
