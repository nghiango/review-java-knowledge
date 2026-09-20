package lab.springcore.questions;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Q08: Demonstrates Spring ApplicationEventPublisher and @EventListener. */
@SuppressWarnings("unused")
public class Q08ApplicationEventsExample {

    public record UserRegisteredEvent(String username) {}

    @Component
    static class UserNotificationListener {
        private String lastNotifiedUser;

        @EventListener
        public void onUserRegistered(UserRegisteredEvent event) {
            this.lastNotifiedUser = event.username();
        }

        public String getLastNotifiedUser() {
            return lastNotifiedUser;
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(UserNotificationListener.class);
            context.refresh();

            ApplicationEventPublisher publisher = context;
            publisher.publishEvent(new UserRegisteredEvent("alice"));

            UserNotificationListener listener = context.getBean(UserNotificationListener.class);
            String notified = listener.getLastNotifiedUser(); // "alice"
        }
    }
}
