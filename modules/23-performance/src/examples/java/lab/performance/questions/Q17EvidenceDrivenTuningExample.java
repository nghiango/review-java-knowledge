package lab.performance.questions;

public final class Q17EvidenceDrivenTuningExample {
    public static void main(String[] args) {
        boolean baseline = true, profile = true, oneChange = true;
        boolean validExperiment = baseline && profile && oneChange; // true
        System.out.println(validExperiment);
    }
}
