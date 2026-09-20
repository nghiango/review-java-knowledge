package lab.resilience.questions;

public class Q09FallbackPatternsAndGracefulDegradationExample {

    record ProductRecommendation(String productId, String source) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Fallback strategies: Cached value, default stub, silent omission
        boolean mlRecommendationServiceFailed = true;

        ProductRecommendation recommendation;
        if (mlRecommendationServiceFailed) {
            // Graceful degradation: return static popular items
            recommendation = new ProductRecommendation("DEFAULT-TOP-SELLER", "STATIC_FALLBACK");
        } else {
            recommendation = new ProductRecommendation("ML-123", "PERSONALIZED_ML");
        }

        boolean isDegraded = "STATIC_FALLBACK".equals(recommendation.source()); // true
        System.out.println("Recommendation degraded gracefully: " + isDegraded);
    }
}
