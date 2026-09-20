package lab.performance.questions;

public final class Q08BenchmarkExample {
    public static void main(String[] args) {
        boolean hasWarmup = true;
        boolean consumesResult = true;
        boolean credible = hasWarmup && consumesResult; // true
        System.out.println(credible);
    }
}
