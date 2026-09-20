package lab.testing.questions;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Q23: Integration tests pass, production fails after a database upgrade.
 *
 * <p>Symptom: the integration suite is green on the embedded substitute, and after the PostgreSQL
 * upgrade one account lookup fails for addresses typed with a capital letter. The test was not
 * wrong about the code, it was wrong about the database: the substitute answered case-insensitively
 * and in insertion order, PostgreSQL compares {@code text} case-sensitively and returns rows in
 * whatever order it likes. The fix is to assert the *contract* — an address is looked up through
 * its normalised form — and to run the slice against a real container ({@code @ServiceConnection},
 * {@code replace = NONE}) so the substitute's semantics never stand in for the store's.
 */
public class Q23PostUpgradeProductionFailureIncident {

    /** The lookup both stores offer, whatever their comparison semantics are. */
    interface AccountStore {

        Optional<Long> balanceByEmail(String email);
    }

    /** The embedded substitute: case-insensitive lookup, which the real store does not promise. */
    static final class EmbeddedStore implements AccountStore {

        private final Map<String, Long> balances = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        EmbeddedStore() {
            balances.put("ada@example.com", 2_500L);
        }

        @Override
        public Optional<Long> balanceByEmail(String email) {
            return Optional.ofNullable(balances.get(email));
        }
    }

    /** PostgreSQL: the unique index and the derived query compare text case-sensitively. */
    static final class PostgresStore implements AccountStore {

        private final Map<String, Long> balances = Map.of("ada@example.com", 2_500L);

        @Override
        public Optional<Long> balanceByEmail(String email) {
            return Optional.ofNullable(balances.get(email));
        }
    }

    public static void main(String[] args) {
        AccountStore embedded = new EmbeddedStore();
        AccountStore postgres = new PostgresStore();

        String asTheUserTypedIt = "Ada@Example.com"; // mixed case, no stray whitespace
        String padded = "  ada@example.com  "; // the same address with whitespace around it

        // Green in the suite, because the substitute answers case-insensitively.
        boolean integrationTestPassed =
                embedded.balanceByEmail(asTheUserTypedIt).isPresent(); // true
        // Red in production, because PostgreSQL does not.
        boolean productionFailed = postgres.balanceByEmail(asTheUserTypedIt).isEmpty(); // true

        // The substitute is forgiving in exactly one dimension, which is why the divergence went
        // unnoticed: whitespace defeats it too, so normalisation was always the application's job.
        boolean paddedFailsOnTheSubstitute = embedded.balanceByEmail(padded).isEmpty(); // true

        // The fix is in the service, not the database: normalise before the query, which is what
        // AccountService.register/balance already do.
        String normalised = padded.trim().toLowerCase(Locale.ROOT); // "ada@example.com"
        boolean fixWorksOnPostgres = postgres.balanceByEmail(normalised).isPresent(); // true
        boolean fixWorksOnTheSubstitute = embedded.balanceByEmail(normalised).isPresent(); // true

        // A second divergence the upgrade exposed: the slice created its schema from the entities
        // (ddl-auto=create-drop) while production is migrated by Flyway, so the collation and the
        // unique index were never the same objects.
        int schemaVersionInTests = 1; // created from the entity mappings
        int schemaVersionInProduction = 2; // Flyway V2 changed the collation
        boolean schemaDiverged = schemaVersionInTests != schemaVersionInProduction; // true

        int assertionsThatTestedTheSubstitute =
                3; // case-insensitive lookup, insertion order, types
        int assertionsThatTestedTheContract = 1; // the balance is returned for a normalised address
        boolean theTestWasWrongNotTheCode = integrationTestPassed && productionFailed; // true

        System.out.println("Test passed: " + integrationTestPassed); // Test passed: true
        System.out.println("Prod failed: " + productionFailed); // Prod failed: true
        System.out.println("Padded: " + paddedFailsOnTheSubstitute); // Padded: true
        System.out.println("Fix on PG: " + fixWorksOnPostgres); // Fix on PG: true
        System.out.println("Fix on fake: " + fixWorksOnTheSubstitute); // Fix on fake: true
        System.out.println("Schema diverged: " + schemaDiverged); // Schema diverged: true
        System.out.println(
                "Substitute asserts: "
                        + assertionsThatTestedTheSubstitute); // Substitute asserts: 3
        System.out.println(
                "Contract asserts: " + assertionsThatTestedTheContract); // Contract asserts: 1
        System.out.println("Test was wrong: " + theTestWasWrongNotTheCode); // Test was wrong: true
    }
}
