package lab.performance.questions;

public final class Q23LatencyIncidentExample {
    public static void main(String[] args) {
        long allocationMbPerSecond = 900, gcPauseP99Ms = 240;
        boolean gcSuspect = allocationMbPerSecond > 500 && gcPauseP99Ms > 200; // true
        System.out.println(gcSuspect);
    }
}
