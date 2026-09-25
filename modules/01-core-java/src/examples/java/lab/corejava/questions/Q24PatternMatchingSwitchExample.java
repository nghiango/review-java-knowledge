package lab.corejava.questions;

@SuppressWarnings("unused")
public final class Q24PatternMatchingSwitchExample {
    private Q24PatternMatchingSwitchExample() {}

    public sealed interface PaymentMethod permits CreditCard, BankTransfer, Crypto {}
    public record CreditCard(String cardNumber, double limit) implements PaymentMethod {}
    public record BankTransfer(String iban) implements PaymentMethod {}
    public record Crypto(String walletAddress, boolean verified) implements PaymentMethod {}

    public static String processPayment(PaymentMethod payment) {
        return switch (payment) {
            case null -> "Payment required"; // Explicit null handling in switch
            case CreditCard(var num, var limit) when limit > 10_000.0 ->
                "Premium Card: " + num; // Guarded pattern with record deconstruction
            case CreditCard(var num, var limit) ->
                "Standard Card: " + num; // Standard record pattern
            case BankTransfer(var iban) ->
                "Bank: " + iban; // Exhaustive decomposition
            case Crypto(var addr, var verified) when verified ->
                "Verified Crypto: " + addr; // Pattern matching on boolean component
            case Crypto(var addr, var verified) ->
                "Unverified Crypto - rejected";
        };
    }

    public static void main(String[] args) {
        String res1 = processPayment(new CreditCard("4111", 15_000.0)); // "Premium Card: 4111"
        String res2 = processPayment(null); // "Payment required"
        String res3 = processPayment(new Crypto("0x123", true)); // "Verified Crypto: 0x123"
    }
}
