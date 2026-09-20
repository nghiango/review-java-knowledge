package lab.testing.questions;

import java.util.List;

/**
 * Q21: Consumer-driven contracts in a microservice fleet.
 *
 * <p>With consumer-driven contracts the <em>consumer</em> publishes the interactions it relies on
 * (request, expected response) and the provider verifies them in its own pipeline. The broker
 * stores one contract per consumer/version and answers {@code can-i-deploy}: a provider version may
 * be promoted only if every consumer contract it must satisfy has been verified against it. That is
 * what makes a rename on the provider side fail <em>before</em> the consumer breaks — a
 * provider-side schema test cannot do it, because only the consumer knows which fields it actually
 * reads. Contract tests do not replace integration tests: they say nothing about the store's
 * semantics.
 */
public class Q21ConsumerDrivenContractsInMicroservices {

    /** One interaction the consumer expects: a request and the response it needs back. */
    record Interaction(String description, String request, String requiredResponse) {}

    /** A provider build, as the broker's verification result describes it. */
    record ProviderVersion(String version, String lookupBody, int unknownStatus) {}

    public static void main(String[] args) {
        List<Interaction> contract =
                List.of(
                        new Interaction(
                                "stock lookup",
                                "GET /inventory/A-1",
                                "200 {\"sku\":\"A-1\",\"available\":3}"),
                        new Interaction("unknown sku", "GET /inventory/UNKNOWN", "404 {}"));

        ProviderVersion current =
                new ProviderVersion("1.4.0", "{\"sku\":\"A-1\",\"available\":3}", 404);
        ProviderVersion renamed = new ProviderVersion("2.0.0", "{\"sku\":\"A-1\",\"qty\":3}", 404);

        int currentSatisfied = satisfiedInteractions(current, contract); // 2
        int renamedSatisfied = satisfiedInteractions(renamed, contract); // 1
        boolean currentIsDeployable = currentSatisfied == contract.size(); // true
        boolean renamedIsDeployable = renamedSatisfied == contract.size(); // false
        String breakingInteraction = firstUnsatisfied(renamed, contract); // "stock lookup"

        System.out.println("Interactions: " + contract.size()); // Interactions: 2
        System.out.println("1.4.0 satisfied: " + currentSatisfied); // 1.4.0 satisfied: 2
        System.out.println("2.0.0 satisfied: " + renamedSatisfied); // 2.0.0 satisfied: 1
        System.out.println("1.4.0 deployable: " + currentIsDeployable); // 1.4.0 deployable: true
        System.out.println("2.0.0 deployable: " + renamedIsDeployable); // 2.0.0 deployable: false
        System.out.println("Breaking: " + breakingInteraction); // Breaking: stock lookup
    }

    /** How many interactions of {@code contract} this provider build satisfies. */
    private static int satisfiedInteractions(ProviderVersion provider, List<Interaction> contract) {
        return (int)
                contract.stream().filter(interaction -> satisfies(provider, interaction)).count();
    }

    /** The description of the first interaction this provider build breaks. */
    private static String firstUnsatisfied(ProviderVersion provider, List<Interaction> contract) {
        return contract.stream()
                .filter(interaction -> !satisfies(provider, interaction))
                .map(Interaction::description)
                .findFirst()
                .orElseThrow();
    }

    /** The provider satisfies an interaction when its response carries the required payload. */
    private static boolean satisfies(ProviderVersion provider, Interaction interaction) {
        if (interaction.request().contains("/inventory/UNKNOWN")) {
            return provider.unknownStatus() == 404;
        }
        return interaction.requiredResponse().contains(provider.lookupBody());
    }
}
