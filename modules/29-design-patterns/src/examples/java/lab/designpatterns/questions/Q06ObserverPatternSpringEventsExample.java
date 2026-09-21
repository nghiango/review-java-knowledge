package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q06: Observer Pattern (Publish-Subscribe). Demonstrates subject publishing domain events to
 * registered observers without tight coupling.
 */
public class Q06ObserverPatternSpringEventsExample {

    public record UserRegisteredEvent(String username, String email) {}

    public interface Observer<E> {
        void onEvent(E event);
    }

    public static class EventPublisher<E> {
        private final List<Observer<E>> observers = new ArrayList<>();

        public void subscribe(Observer<E> observer) {
            observers.add(observer);
        }

        public void publish(E event) {
            for (Observer<E> observer : observers) {
                observer.onEvent(event);
            }
        }
    }

    public static void main(String[] args) {
        EventPublisher<UserRegisteredEvent> publisher = new EventPublisher<>();
        List<String> notificationLog = new ArrayList<>();

        // Observer 1: Welcome Email
        publisher.subscribe(event -> notificationLog.add("EmailSent:" + event.email()));
        // Observer 2: Audit Trail
        publisher.subscribe(event -> notificationLog.add("AuditLogged:" + event.username()));

        publisher.publish(new UserRegisteredEvent("john", "john@example.com"));

        boolean notifiedAll = notificationLog.size() == 2; // true
        boolean emailTriggered = notificationLog.contains("EmailSent:john@example.com"); // true

        System.out.println("Q06 notifiedAll: " + notifiedAll + ", email: " + emailTriggered);
    }
}
