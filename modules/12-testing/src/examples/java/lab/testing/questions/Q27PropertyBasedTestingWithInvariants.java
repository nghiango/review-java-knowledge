package lab.testing.questions;

import java.util.List;

/**
 * Q27: How does property-based testing differ from example-based unit testing in discovering
 * edge cases, shrinking failures, and verifying invariants?
 */
public class Q27PropertyBasedTestingWithInvariants {

    // Domain operation under test: Money allocation preserving total sum exactly
    static List<Long> allocateCents(long totalCents, int numParts) {
        if (numParts <= 0 || totalCents < 0) {
            throw new IllegalArgumentException("Invalid allocation parameters");
        }
        long baseShare = totalCents / numParts;
        long remainder = totalCents % numParts;

        java.util.ArrayList<Long> shares = new java.util.ArrayList<>();
        for (int i = 0; i < numParts; i++) {
            shares.add(baseShare + (i < remainder ? 1 : 0));
        }
        return shares;
    }

    public static void main(String[] args) {
        // Property 1 (Conservation Invariant): Sum of allocated parts MUST equal original total
        long testTotal = 1000L;
        int parts = 3;
        List<Long> shares = allocateCents(testTotal, parts); // [334, 333, 333]

        long sumOfShares = shares.stream().mapToLong(Long::longValue).sum(); // 1000L
        boolean invariantHolds = (sumOfShares == testTotal); // true

        // Property 2: Difference between max and min share cannot exceed 1 cent
        long min = shares.stream().mapToLong(Long::longValue).min().orElse(0); // 333
        long max = shares.stream().mapToLong(Long::longValue).max().orElse(0); // 334
        boolean fairDistribution = (max - min <= 1); // true

        System.out.println("Allocated Shares: " + shares); // [334, 333, 333]
        System.out.println("Conservation Invariant Preserved: " + invariantHolds); // true
        System.out.println("Fair Distribution Invariant Preserved: " + fairDistribution); // true
    }
}
