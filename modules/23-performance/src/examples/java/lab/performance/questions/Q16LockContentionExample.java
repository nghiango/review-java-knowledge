package lab.performance.questions;

public final class Q16LockContentionExample {
    public static void main(String[] args) {
        long blockedMs = 800, wallMs = 1_000;
        double blockedRatio = (double) blockedMs / wallMs; // 0.8
        System.out.println(blockedRatio);
    }
}
