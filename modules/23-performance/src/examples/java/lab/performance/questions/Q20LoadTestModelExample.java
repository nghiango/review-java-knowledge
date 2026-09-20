package lab.performance.questions;

public final class Q20LoadTestModelExample {
    public static void main(String[] args) {
        int steadyUsers = 100, spikeUsers = 400;
        boolean testsSaturation = spikeUsers > steadyUsers; // true
        System.out.println(testsSaturation);
    }
}
