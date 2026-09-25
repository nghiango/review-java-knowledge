package lab.springboot.questions;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public final class Q29KubernetesGracefulShutdownScenarioExample {
    private Q29KubernetesGracefulShutdownScenarioExample() {}

    public static class GracefulShutdownTracker {
        private final AtomicBoolean acceptingTraffic = new AtomicBoolean(true);
        private final AtomicBoolean activeRequestsDrained = new AtomicBoolean(false);

        // When SIGTERM is received by Spring Boot (with server.shutdown=graceful):
        // 1. Embedded Tomcat/Jetty stops accepting new incoming socket connections
        // 2. Active in-flight HTTP requests are given a grace period to complete
        //    configured by spring.lifecycle.timeout-per-shutdown-phase=30s
        // 3. Once requests drain, ApplicationContext close callbacks and DataSource pools shut down
        public void initiateShutdown() {
            acceptingTraffic.set(false);
            // Wait for in-flight requests to complete
            activeRequestsDrained.set(true);
        }

        public boolean isAcceptingTraffic() {
            return acceptingTraffic.get();
        }

        public boolean isDrained() {
            return activeRequestsDrained.get();
        }
    }

    public static void main(String[] args) {
        GracefulShutdownTracker tracker = new GracefulShutdownTracker();
        tracker.initiateShutdown();

        // In Kubernetes, a preStop hook (e.g. `sleep 10`) is essential to allow kube-proxy and
        // ingress
        // controllers to deregister the pod endpoint before the container begins shutting down its
        // server.
        boolean accepting = tracker.isAcceptingTraffic(); // false
        boolean drained = tracker.isDrained(); // true
    }
}
