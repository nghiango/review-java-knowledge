package lab.corejava.questions;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public final class Q05OptionalUsageExample {
    private Q05OptionalUsageExample() {}

    private static final Map<String, String> REPO = Map.of("cust-1", "Ada Lovelace");

    public static Optional<String> findCustomerName(String id) {
        return Optional.ofNullable(REPO.get(id));
    }

    public static void main(String[] args) {
        String existing =
                findCustomerName("cust-1")
                        .map(String::toUpperCase)
                        .orElse("UNKNOWN"); // "ADA LOVELACE"

        String missing =
                findCustomerName("cust-99").map(String::toUpperCase).orElse("UNKNOWN"); // "UNKNOWN"
    }
}
