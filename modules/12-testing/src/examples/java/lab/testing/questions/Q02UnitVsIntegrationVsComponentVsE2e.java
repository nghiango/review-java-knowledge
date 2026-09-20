package lab.testing.questions;

import java.util.List;
import java.util.Map;

/**
 * Q02: Unit vs integration vs component vs end-to-end — what each level can actually observe.
 *
 * <p>The levels differ by *how much real machinery is in the test*, not by the framework used. In
 * this module {@code CheckoutService} with a hand-written gateway is a unit test, {@code
 * OrderFulfilmentService} against a WireMock stub is a component test, {@code AccountRepositoryIT}
 * against a PostgreSQL container is an integration test, and a request through a deployed instance
 * is end-to-end. A defect should be owned by the cheapest level that can observe it.
 */
public class Q02UnitVsIntegrationVsComponentVsE2e {

    /** One level of the suite, described by how far its collaborators reach. */
    record Level(
            String name,
            String scope,
            boolean needsInfrastructure,
            boolean crossesProcessBoundary) {}

    public static void main(String[] args) {
        List<Level> levels =
                List.of(
                        new Level("unit", "one class or pure function", false, false),
                        new Level(
                                "component", "real service logic, boundary stubbed", false, false),
                        new Level(
                                "integration",
                                "service plus real PostgreSQL or broker",
                                true,
                                true),
                        new Level(
                                "end-to-end",
                                "the deployed system via its public API",
                                true,
                                true));

        // The cheapest level that can observe a defect is the level that should own it.
        Map<String, String> ownerPerDefect =
                Map.of(
                        "rounding rule in PricingCalculator", "unit",
                        "JSON field renamed on the wire", "component",
                        "unique index missing after migration", "integration",
                        "bean wiring absent in the prod profile", "end-to-end");

        long infrastructureLevels = levels.stream().filter(Level::needsInfrastructure).count(); // 2
        long inProcessLevels = levels.size() - infrastructureLevels; // 2
        String indexOwner =
                ownerPerDefect.get("unique index missing after migration"); // "integration"
        String wiringOwner =
                ownerPerDefect.get("bean wiring absent in the prod profile"); // "end-to-end"
        long unitOwnedDefects =
                ownerPerDefect.values().stream().filter("unit"::equals).count(); // 1

        System.out.println("Levels: " + levels.size()); // Levels: 4
        System.out.println("Infra levels: " + infrastructureLevels); // Infra levels: 2
        System.out.println("In-process: " + inProcessLevels); // In-process: 2
        System.out.println("Index owner: " + indexOwner); // Index owner: integration
        System.out.println("Wiring owner: " + wiringOwner); // Wiring owner: end-to-end
        System.out.println("Unit-owned: " + unitOwnedDefects); // Unit-owned: 1
    }
}
