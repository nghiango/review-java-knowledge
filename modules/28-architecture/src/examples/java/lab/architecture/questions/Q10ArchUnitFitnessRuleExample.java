package lab.architecture.questions;

/**
 * Q10: ArchUnit Rules for Architectural Fitness Functions. Demonstrates architectural rule concept
 * in Java test suites.
 */
public class Q10ArchUnitFitnessRuleExample {

    public static class ArchitecturalRuleValidator {
        public static boolean isPackageAllowedToAccess(String fromPackage, String toPackage) {
            // Rule: domain cannot access infrastructure
            if (fromPackage.contains(".domain.") && toPackage.contains(".infrastructure.")) {
                return false;
            }
            return true;
        }
    }

    public static void main(String[] args) {
        boolean validAccess =
                ArchitecturalRuleValidator.isPackageAllowedToAccess(
                        "lab.architecture.application", "lab.architecture.domain"); // true

        boolean illegalAccess =
                ArchitecturalRuleValidator.isPackageAllowedToAccess(
                        "lab.architecture.domain.model",
                        "lab.architecture.infrastructure.adapter"); // false (violates hexagonal
        // architecture boundary)

        System.out.println("Q10 valid: " + validAccess + ", illegal: " + illegalAccess);
    }
}
