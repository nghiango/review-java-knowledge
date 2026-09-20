package lab.webflux.noerrorhandler;

import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CorrectCartPricingEngine {

    private final WebClient loyaltyClient;
    private final WebClient couponClient;

    public CorrectCartPricingEngine(
            WebClient.Builder builder, String loyaltyUrl, String couponUrl) {
        this.loyaltyClient = builder.baseUrl(loyaltyUrl).build();
        this.couponClient = builder.baseUrl(couponUrl).build();
    }

    public Mono<FinalCartPrice> calculateCartPrice(String userId, double baseTotal) {
        // Isolated error handling: loyalty discount falls back to 0.0 on failure or timeout
        Mono<Double> loyaltyDiscountMono =
                loyaltyClient
                        .get()
                        .uri("/loyalty/{user}/discount", userId)
                        .retrieve()
                        .bodyToMono(LoyaltyDiscount.class)
                        .map(LoyaltyDiscount::discountAmount)
                        .timeout(Duration.ofSeconds(2))
                        .onErrorReturn(0.0);

        // Isolated error handling: coupon discount falls back to 0.0 on failure or timeout
        Mono<Double> couponDiscountMono =
                couponClient
                        .get()
                        .uri("/coupons/{user}/best", userId)
                        .retrieve()
                        .bodyToMono(CouponDiscount.class)
                        .map(CouponDiscount::couponValue)
                        .timeout(Duration.ofSeconds(2))
                        .onErrorReturn(0.0);

        // Mono.zip succeeds even if either external service is down or times out
        return Mono.zip(loyaltyDiscountMono, couponDiscountMono)
                .map(
                        tuple -> {
                            double loyalty = tuple.getT1();
                            double coupon = tuple.getT2();
                            double totalDiscount = loyalty + coupon;
                            return new FinalCartPrice(
                                    userId,
                                    baseTotal,
                                    totalDiscount,
                                    Math.max(0, baseTotal - totalDiscount));
                        });
    }

    public record LoyaltyDiscount(String userId, double discountAmount) {}

    public record CouponDiscount(String userId, double couponValue) {}

    public record FinalCartPrice(
            String userId, double originalTotal, double discount, double finalTotal) {}
}
