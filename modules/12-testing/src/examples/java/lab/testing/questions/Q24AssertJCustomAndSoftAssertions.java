package lab.testing.questions;

import org.assertj.core.api.SoftAssertions;

/**
 * Q24: How do AssertJ custom assertions and SoftAssertions improve failure diagnostic clarity
 * compared to chained multiple assertions?
 */
public class Q24AssertJCustomAndSoftAssertions {

    record CustomerAccount(String id, String email, int balanceCents, boolean active) {}

    public static void main(String[] args) {
        CustomerAccount account = new CustomerAccount("acc-123", "alice@example.com", 5000, true);

        // Standard hard assertions: if first fails, remaining checks never execute
        boolean hasId = account.id().startsWith("acc-"); // true
        boolean emailValid = account.email().contains("@"); // true

        // SoftAssertions: collects all failures in one run, printing complete report
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(account.id()).startsWith("acc-");
        softly.assertThat(account.email()).contains("@");
        softly.assertThat(account.balanceCents()).isGreaterThan(0);
        softly.assertThat(account.active()).isTrue();
        softly.assertAll(); // Verifies all assertions and passes cleanly

        boolean allPassed = softly.wasSuccess(); // true

        System.out.println("Hard Assertion ID: " + hasId); // true
        System.out.println("Email Valid: " + emailValid); // true
        System.out.println("Soft Assertions All Passed: " + allPassed); // true
    }
}
