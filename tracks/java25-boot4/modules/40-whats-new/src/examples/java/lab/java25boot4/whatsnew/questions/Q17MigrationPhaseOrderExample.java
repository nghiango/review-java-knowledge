package lab.java25boot4.whatsnew.questions;

/** Q17: in what order do you migrate Java 21 / Boot 3.5 to Java 25 / Boot 4, and why? */
public class Q17MigrationPhaseOrderExample {

    public static void main(String[] args) {
        // One axis per phase so a regression can be attributed to a single change.
        String[] phases = {
            "phase 1: Java 21 -> Java 25 on Boot 3.5 (runtime only)",
            "phase 2: Boot 3.5 -> Boot 4.0 (framework only)",
            "phase 3: verify, canary, compare p99 / GC / pool metrics"
        };

        for (String phase : phases) {
            System.out.println(phase); // three phases, printed in order
        }
        System.out.println(phases.length); // 3 — never jump straight from Java 21 to Java 25 + Boot 4
    }
}
