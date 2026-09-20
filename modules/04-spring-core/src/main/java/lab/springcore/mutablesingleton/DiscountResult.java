package lab.springcore.mutablesingleton;

public record DiscountResult(
        String customerId, double totalDiscount, double tierDiscount, double loyaltyDiscount) {}
