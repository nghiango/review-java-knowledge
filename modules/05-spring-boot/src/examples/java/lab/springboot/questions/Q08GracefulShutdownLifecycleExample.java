package lab.springboot.questions;

import java.util.concurrent.atomic.AtomicBoolean;

public class Q08GracefulShutdownLifecycleExample {

    enum ShutdownPhase {
        STOP_ACCEPTING_NEW_REQUESTS,
        DRAIN_INFLIGHT_REQUESTS,
        RELEASE_RESOURCES,
        JVM_EXIT
    }

    static class GracefulShutdownCoordinator {
        private final AtomicBoolean acceptingTraffic = new AtomicBoolean(true);

        public void initiateShutdown() {
            acceptingTraffic.set(false); // Stop routing new requests
        }

        public boolean isAcceptingTraffic() {
            return acceptingTraffic.get();
        }
    }

    public static void main(String[] args) {
        GracefulShutdownCoordinator coordinator = new GracefulShutdownCoordinator();
        boolean initialStatus = coordinator.isAcceptingTraffic(); // true

        coordinator.initiateShutdown();
        boolean afterShutdownStatus = coordinator.isAcceptingTraffic(); // false

        System.out.println(
                "Initial status: " + initialStatus + ", after SIGTERM: " + afterShutdownStatus);
    }
}
