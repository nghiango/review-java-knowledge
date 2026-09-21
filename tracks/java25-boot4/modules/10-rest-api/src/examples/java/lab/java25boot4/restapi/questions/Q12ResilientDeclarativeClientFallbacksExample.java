package lab.java25boot4.restapi.questions;

/**
 * Q12: How do you implement timeout isolation and fallback mechanisms when calling external
 * microservices via declarative HTTP interfaces?
 */
public class Q12ResilientDeclarativeClientFallbacksExample {

    public record RateQuote(String currency, double rate, boolean isFallback) {}

    public static class ResilientFxClient {
        public RateQuote getExchangeRate(String currency, boolean simulateFailure) {
            if (simulateFailure) {
                // Fallback to cached reference rate upon timeout / circuit open
                return new RateQuote(currency, 1.05, true);
            }
            return new RateQuote(currency, 1.08, false);
        }
    }

    public static void main(String[] args) {
        ResilientFxClient client = new ResilientFxClient();

        RateQuote primary = client.getExchangeRate("EUR", false);
        RateQuote fallback = client.getExchangeRate("EUR", true);

        System.out.println("Primary rate: " + primary.rate()); // 1.08
        System.out.println("Primary fallback: " + primary.isFallback()); // false
        System.out.println("Fallback rate: " + fallback.rate()); // 1.05
        System.out.println("Fallback active: " + fallback.isFallback()); // true
    }
}
