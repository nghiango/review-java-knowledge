package lab.testing.questions;

import lab.testing.pricing.Money;
import lab.testing.pricing.PricingCalculator;

/**
 * Q20: Mutation testing with PIT — what a mutation score adds over coverage.
 *
 * <p>PIT rewrites the bytecode of the classes under test, one small mutation at a time (drop the
 * {@code + 50} rounding term, flip a comparison, remove a call), then runs the suite against each
 * mutant. A mutant that the suite still passes has <em>survived</em>: either the assertion was too
 * weak to notice, or no test reached the code. Coverage cannot tell the two apart — a line can be
 * executed by a test that asserts nothing about it. The score is {@code killed / (generated -
 * noCoverage)}, and the useful output is the list of survivors, not the percentage.
 */
public class Q20MutationTestingWithPit {

    public static void main(String[] args) {
        PricingCalculator calculator = new PricingCalculator();
        Money real = calculator.applyPercentDiscount(new Money(1250), 33); // Money[cents=838]

        // The mutant PIT generates by dropping the "+ 50" half-up rounding term.
        long mutatedCents = (1250L * (100L - 33L)) / 100L; // 837
        Money mutated = new Money(mutatedCents); // Money[cents=837]

        // A weak assertion ("the amount is not negative") is satisfied by the real value *and* by
        // the mutant, so it never fails on the mutant and the mutant survives — the suite stays
        // green with the bug in place.
        boolean weakAssertionHoldsOnTheMutant = mutated.cents() >= 0; // true
        boolean weakAssertionKillsTheMutant = !weakAssertionHoldsOnTheMutant; // false
        // An assertion on the exact amount kills it.
        boolean strongAssertionKillsTheMutant =
                real.equals(new Money(838)) && !real.equals(mutated); // true

        int generatedMutants = 48;
        int noCoverage = 3; // no test reaches the mutated line
        int killed = 31;
        int survivors = generatedMutants - noCoverage - killed; // 14
        int mutationScore = killed * 100 / (generatedMutants - noCoverage); // 68
        int lineCoveragePercent = 92; // coverage says the line ran; PIT says the assertion noticed
        boolean coverageHidesWeakAssertions = lineCoveragePercent > mutationScore; // true

        System.out.println("Real: " + real); // Real: Money[cents=838]
        System.out.println("Mutant: " + mutated); // Mutant: Money[cents=837]
        System.out.println("Weak kills: " + weakAssertionKillsTheMutant); // Weak kills: false
        System.out.println("Strong kills: " + strongAssertionKillsTheMutant); // Strong kills: true
        System.out.println("Survivors: " + survivors); // Survivors: 14
        System.out.println("Mutation score: " + mutationScore + "%"); // Mutation score: 68%
        System.out.println("Line coverage: " + lineCoveragePercent + "%"); // Line coverage: 92%
        System.out.println(
                "Coverage hides it: " + coverageHidesWeakAssertions); // Coverage hides it: true
    }
}
