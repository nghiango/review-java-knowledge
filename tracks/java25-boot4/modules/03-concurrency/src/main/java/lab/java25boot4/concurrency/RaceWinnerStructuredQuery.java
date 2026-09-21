package lab.java25boot4.concurrency;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

/**
 * Demonstrates StructuredTaskScope.open() with Joiner.anySuccessfulResultOrThrow() for
 * fastest-first queries. Cancels remaining slow replica requests once the first valid result is
 * obtained.
 */
public class RaceWinnerStructuredQuery {

    public record Quote(String provider, int priceUsd) {}

    public Quote getFastestQuote(long delayMs1, long delayMs2) throws Throwable {
        try (var scope = StructuredTaskScope.open(Joiner.<Quote>anySuccessfulResultOrThrow())) {
            scope.fork(() -> fetchFromProvider("Alpha", 100, delayMs1));
            scope.fork(() -> fetchFromProvider("Beta", 102, delayMs2));

            return scope.join();
        }
    }

    private Quote fetchFromProvider(String provider, int price, long delayMs)
            throws InterruptedException {
        Thread.sleep(delayMs);
        return new Quote(provider, price);
    }
}
