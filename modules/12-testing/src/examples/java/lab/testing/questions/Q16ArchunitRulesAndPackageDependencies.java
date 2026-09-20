package lab.testing.questions;

import java.util.List;
import java.util.Set;

/**
 * Q16: ArchUnit rules and package dependencies.
 *
 * <p>ArchUnit imports the compiled classes (ASM reads the constant pool, so every referenced type
 * is a dependency) and then evaluates rules over that graph: {@code
 * noClasses().that().resideInAPackage("..pricing..").should().dependOnClassesThat()
 * .resideInAPackage("..accounts..")}. A rule that describes the layering catches the import that
 * quietly couples two packages — something no behavioural test notices. {@code FreezingArchRule}
 * records today's violations so a legacy codebase can adopt a rule and fail only on *new* ones.
 */
public class Q16ArchunitRulesAndPackageDependencies {

    /** One class as ArchUnit sees it after importing bytecode: its name and what it depends on. */
    record JavaClass(String name, Set<String> dependsOn) {}

    /**
     * A rule: classes in {@code originPackage} must not depend on classes in {@code
     * forbiddenPackage}.
     */
    record Rule(String description, String originPackage, String forbiddenPackage) {}

    public static void main(String[] args) {
        // The dependency graph of this module, as ASM reads it from the class files. Package
        // matching here is a simplification of ArchUnit's ".." (which also matches subpackages).
        List<JavaClass> classes =
                List.of(
                        new JavaClass(
                                "lab.testing.pricing.CheckoutService",
                                Set.of(
                                        "lab.testing.pricing.PricingCalculator",
                                        "lab.testing.pricing.PaymentGateway",
                                        "lab.testing.pricing.Money",
                                        "java.util.List")),
                        new JavaClass(
                                "lab.testing.pricing.PricingCalculator",
                                Set.of(
                                        "lab.testing.pricing.Money",
                                        "java.util.List",
                                        "java.util.Objects")),
                        new JavaClass(
                                "lab.testing.orders.OrderTotals",
                                Set.of(
                                        "lab.testing.pricing.Money",
                                        "lab.testing.orders.Order",
                                        "lab.testing.orders.OrderLine")),
                        new JavaClass(
                                "lab.testing.orders.OrderPricingAdapter",
                                Set.of(
                                        "lab.testing.accounts.AccountService",
                                        "lab.testing.pricing.Money")),
                        new JavaClass(
                                "lab.testing.fulfilment.OrderFulfilmentService",
                                Set.of(
                                        "lab.testing.fulfilment.InventoryClient",
                                        "lab.testing.fulfilment.InventoryResponse")),
                        new JavaClass(
                                "lab.testing.accounts.AccountService",
                                Set.of(
                                        "lab.testing.accounts.Account",
                                        "lab.testing.accounts.AccountRepository",
                                        "org.springframework.stereotype.Service")),
                        new JavaClass(
                                "lab.testing.accounts.AccountRepository",
                                Set.of(
                                        "lab.testing.accounts.Account",
                                        "org.springframework.data.jpa.repository.JpaRepository")));

        Rule pricingMustNotKnowAccounts =
                new Rule(
                        "pricing must not depend on accounts",
                        "lab.testing.pricing",
                        "lab.testing.accounts");
        Rule ordersMustNotKnowAccounts =
                new Rule(
                        "orders must not depend on accounts",
                        "lab.testing.orders",
                        "lab.testing.accounts");

        List<String> pricingViolations = violations(classes, pricingMustNotKnowAccounts); // none
        List<String> orderViolations = violations(classes, ordersMustNotKnowAccounts); // one class

        int frozenViolations = 1; // the baseline FreezingArchRule recorded when the rule was added
        boolean rulePassesAgainstTheBaseline = orderViolations.size() <= frozenViolations; // true
        String offender = orderViolations.get(0); // the class that breaks the layering
        String offenderSimpleName =
                offender.substring(offender.lastIndexOf('.') + 1); // simple name

        long dependencies =
                classes.stream().mapToLong(clazz -> clazz.dependsOn().size()).sum(); // 19

        System.out.println("Classes analyzed: " + classes.size()); // Classes analyzed: 7
        System.out.println("Dependencies: " + dependencies); // Dependencies: 19
        System.out.println(
                "Pricing violations: " + pricingViolations.size()); // Pricing violations: 0
        System.out.println("Orders violations: " + orderViolations.size()); // Orders violations: 1
        System.out.println("Offender: " + offenderSimpleName); // Offender: OrderPricingAdapter
        System.out.println(
                "Baseline passes: " + rulePassesAgainstTheBaseline); // Baseline passes: true
    }

    /**
     * The classes that break {@code rule}: they live in the origin package and import the other.
     */
    private static List<String> violations(List<JavaClass> classes, Rule rule) {
        return classes.stream()
                .filter(clazz -> clazz.name().startsWith(rule.originPackage()))
                .filter(
                        clazz ->
                                clazz.dependsOn().stream()
                                        .anyMatch(
                                                dependency ->
                                                        dependency.startsWith(
                                                                rule.forbiddenPackage())))
                .map(JavaClass::name)
                .toList();
    }
}
