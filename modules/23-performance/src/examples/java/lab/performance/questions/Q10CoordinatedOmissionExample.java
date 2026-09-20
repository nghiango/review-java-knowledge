package lab.performance.questions;

public final class Q10CoordinatedOmissionExample {
    public static void main(String[] args) {
        int intended = 100, sentDuringPause = 0;
        boolean omitted = intended > 0 && sentDuringPause == 0; // true
        System.out.println(omitted);
    }
}
