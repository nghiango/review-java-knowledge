package lab.springboot.questions;

public class Q23SigtermInflightTaskDropScenarioExample {

    record DeploymentIncident(
            String symptom, String rootCause, int failedTransactions, String correctiveAction) {}

    public static void main(String[] args) {
        // Incident Simulation: 502 Bad Gateway and dropped transactions during Kubernetes rolling
        // update
        DeploymentIncident incident =
                new DeploymentIncident(
                        "502 Bad Gateway spikes and dropped background payment batch tasks during pod termination",
                        "server.shutdown: immediate aborted HTTP connections, and ThreadPoolTaskExecutor was terminated without awaiting in-flight tasks",
                        42,
                        "Configure server.shutdown=graceful, timeout-per-shutdown-phase=30s, and setWaitForTasksToCompleteOnShutdown(true)");

        int drops = incident.failedTransactions(); // 42
        boolean hasGracefulAction =
                incident.correctiveAction().contains("server.shutdown=graceful"); // true

        System.out.println(
                "Incident drops: "
                        + drops
                        + ", root cause: "
                        + incident.rootCause()
                        + ", fix: "
                        + hasGracefulAction);
    }
}
