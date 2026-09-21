package lab.java25boot4.testing.questions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Q12: How can ArchUnit enforce Java 25 architectural boundaries and package-info nullness
 * invariants?
 */
public class Q12ArchUnitJava25RulesExample {

    public static void main(String[] args) {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("lab.java25boot4.testing..")
                        .should()
                        .resideInAPackage("lab.java25boot4.testing..")
                        .allowEmptyShould(true);

        boolean isRuleValid = rule.getDescription().contains("lab.java25boot4.testing");

        System.out.println(
                "Rule description valid: " + isRuleValid); // Rule description valid: true
    }
}
