package lab.performance.questions;

public final class Q09LittlesLawExample {
    public static void main(String[] args) {
        double concurrency = 200 * 0.050; // 10 in-flight at 200 rps and 50 ms
        System.out.println(concurrency);
    }
}
