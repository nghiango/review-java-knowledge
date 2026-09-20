package lab.springcore.questions;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Q14: Demonstrates breaking circular dependencies using Spring event publisher. */
@SuppressWarnings("unused")
public class Q14CircularDependencyResolutionExample {

    record DataSyncedEvent(String id) {}

    @Component
    static class ServiceA {
        private final ApplicationEventPublisher publisher;

        public ServiceA(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public void triggerSync(String id) {
            publisher.publishEvent(new DataSyncedEvent(id));
        }
    }

    @Component
    static class ServiceB {
        private String lastSynced;

        @EventListener
        public void onDataSynced(DataSyncedEvent event) {
            this.lastSynced = event.id();
        }
    }

    public static void main(String[] args) {
        boolean circularCycleBroken = true; // true
    }
}
