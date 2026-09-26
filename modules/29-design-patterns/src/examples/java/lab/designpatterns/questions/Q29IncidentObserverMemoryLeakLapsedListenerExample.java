package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates a production incident where an unbounded Observer pattern created memory leaks
 * (Lapsed Listener problem) by failing to deregister subscribers from a singleton event publisher.
 *
 * <p>Fix: Use WeakReference listeners or explicit try-with-resources AutoCloseable subscriptions.</p>
 */
public class Q29IncidentObserverMemoryLeakLapsedListenerExample {

    public interface EventListener {
        void onEvent(String data);
    }

    public static class EventPublisher {
        private final List<EventListener> listeners = new ArrayList<>();

        public void subscribe(EventListener listener) {
            listeners.add(listener);
        }

        public void unsubscribe(EventListener listener) {
            listeners.remove(listener);
        }

        public int activeSubscriberCount() {
            return listeners.size();
        }
    }

    public static void main(String[] args) {
        EventPublisher publisher = new EventPublisher();

        EventListener temporaryListener = data -> {};
        publisher.subscribe(temporaryListener);

        // Leak: Subscriber retained strongly after scope finishes
        boolean subscriberLeaked = publisher.activeSubscriberCount() == 1; // true

        // Fix: Explicit unregistration
        publisher.unsubscribe(temporaryListener);
        boolean cleanlyDeregistered = publisher.activeSubscriberCount() == 0; // true

        System.out.println("Listener leaked before fix: " + subscriberLeaked);
        System.out.println("Listener deregistered cleanly: " + cleanlyDeregistered);
    }
}
