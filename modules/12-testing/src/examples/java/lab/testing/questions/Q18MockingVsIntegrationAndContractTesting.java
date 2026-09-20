package lab.testing.questions;

import java.util.List;

/**
 * Q18: Balancing mocking against integration and contract testing.
 *
 * <p>A mock answers whatever the test told it to answer, so it can only observe defects in the code
 * the test itself drives — it cannot observe the request the client builds or the response it
 * binds. A stub server (WireMock) can: the client really opens a connection, so a renamed path, a
 * missing {@code Accept} header or a renamed JSON field fails the test. A container-backed test
 * adds the store's semantics, and an end-to-end test adds the deployment. The rule is not "mock
 * less" but "double only at the boundary, and verify the boundary against something that speaks the
 * protocol" — which is what {@code OrderFulfilmentWireMockIT} does and the mocked-client review
 * target does not.
 */
public class Q18MockingVsIntegrationAndContractTesting {

    /** A defect and the cheapest test type that can observe it. */
    record Defect(String description, String observedBy) {}

    public static void main(String[] args) {
        List<Defect> defects =
                List.of(
                        new Defect("rounding rule changed in PricingCalculator", "unit"),
                        new Defect("inventory endpoint renamed to /stock/{sku}", "contract"),
                        new Defect("wire field renamed from available to qty", "contract"),
                        new Defect("unique index dropped by a migration", "integration"),
                        new Defect("bean missing in the prod profile", "end-to-end"));

        List<String> wireLevelDefects =
                List.of(
                        "endpoint renamed to /stock/{sku}",
                        "wire field renamed from available to qty",
                        "status code changed from 200 to 202");

        // A test that mocks InventoryClient never sends a request, so it cannot see any of them.
        long caughtByAMockedClient = 0; // the mock returns the test's own assumption
        long caughtByAStubServer = wireLevelDefects.size(); // 3: the client really calls WireMock

        long unitOwned =
                defects.stream().filter(defect -> defect.observedBy().equals("unit")).count(); // 1
        long contractOwned =
                defects.stream()
                        .filter(defect -> defect.observedBy().equals("contract"))
                        .count(); // 2
        long integrationOwned =
                defects.stream()
                        .filter(defect -> defect.observedBy().equals("integration"))
                        .count(); // 1
        long e2eOwned = defects.size() - unitOwned - contractOwned - integrationOwned; // 1

        System.out.println("Defects: " + defects.size()); // Defects: 5
        System.out.println("Unit-owned: " + unitOwned); // Unit-owned: 1
        System.out.println("Contract-owned: " + contractOwned); // Contract-owned: 2
        System.out.println("Integration-owned: " + integrationOwned); // Integration-owned: 1
        System.out.println("E2E-owned: " + e2eOwned); // E2E-owned: 1
        System.out.println("Mock catches: " + caughtByAMockedClient); // Mock catches: 0
        System.out.println("Stub server catches: " + caughtByAStubServer); // Stub server catches: 3
    }
}
