package lab.springboot.questions;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SuppressWarnings("unused")
public final class Q26StartupPhasesAndLazyInitExample {
    private Q26StartupPhasesAndLazyInitExample() {}

    public static class LifecycleListenerBean {
        private boolean isReady = false;
        private boolean isLive = false;

        // ApplicationReadyEvent: Fired after command-line runners execute and HTTP port binds
        @EventListener
        public void onApplicationReady(ApplicationReadyEvent event) {
            this.isReady = true;
        }

        // AvailabilityChangeEvent: Kubernetes liveness and readiness probe state
        @EventListener
        public void onAvailabilityChange(AvailabilityChangeEvent<?> event) {
            if (event.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
                this.isLive = true;
            }
        }

        public boolean isReady() {
            return isReady;
        }
    }

    public static void main(String[] args) {
        LifecycleListenerBean bean = new LifecycleListenerBean();
        // With spring.main.lazy-initialization=true:
        // Startup time is accelerated, but runtime latency incurs first-request warmup spikes,
        // and configuration/dependency injection failures are concealed until runtime!
        boolean ready = bean.isReady(); // false initially until ApplicationReadyEvent fires
    }
}
