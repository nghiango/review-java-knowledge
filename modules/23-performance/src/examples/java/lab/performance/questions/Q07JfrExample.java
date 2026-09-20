package lab.performance.questions;

public final class Q07JfrExample {
    public static void main(String[] args) {
        String recording = "-XX:StartFlightRecording=settings=profile,duration=60s";
        boolean bounded = recording.contains("duration=60s"); // true
        System.out.println(bounded);
    }
}
