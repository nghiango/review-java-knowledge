package lab.corejava.questions;

import java.util.Optional;

@SuppressWarnings("unused")
public final class Q20OptionalMisuseExample {
    private Q20OptionalMisuseExample() {}

    // Clean API: Accept required value directly, return Optional only for possible absence
    public static Optional<String> extractDiscountCode(String userTier) {
        if (userTier == null) {
            throw new IllegalArgumentException("userTier is required");
        }
        return "VIP".equalsIgnoreCase(userTier) ? Optional.of("DISCOUNT_20") : Optional.empty();
    }

    public static void main(String[] args) {
        String discount = extractDiscountCode("VIP").orElse("NONE"); // "DISCOUNT_20"
        String standard = extractDiscountCode("REGULAR").orElse("NONE"); // "NONE"
    }
}
