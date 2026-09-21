package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q16: Mediator Pattern vs Event Broker. Demonstrates centralized in-process coordination
 * (Mediator) versus decoupled pub-sub message routing (Event Broker).
 */
public class Q16MediatorVsEventBrokerExample {

    // Mediator: Central coordinator explicitly routing messages between colleagues
    public static class ChatMediator {
        private final List<Colleague> colleagues = new ArrayList<>();

        public void register(Colleague colleague) {
            colleagues.add(colleague);
        }

        public void broadcast(String message, Colleague sender) {
            for (Colleague c : colleagues) {
                if (!c.equals(sender)) {
                    c.receive(message);
                }
            }
        }
    }

    public static class Colleague {
        private final String name;
        private final ChatMediator mediator;
        private final List<String> inbox = new ArrayList<>();

        public Colleague(String name, ChatMediator mediator) {
            this.name = name;
            this.mediator = mediator;
            this.mediator.register(this);
        }

        public void send(String message) {
            mediator.broadcast(name + ": " + message, this);
        }

        public void receive(String message) {
            inbox.add(message);
        }

        public List<String> getInbox() {
            return inbox;
        }
    }

    public static void main(String[] args) {
        ChatMediator mediator = new ChatMediator();
        Colleague user1 = new Colleague("Alice", mediator);
        Colleague user2 = new Colleague("Bob", mediator);

        user1.send("Hello Bob");

        boolean receivedByBob = user2.getInbox().contains("Alice: Hello Bob"); // true
        boolean senderNotSelfNotified = user1.getInbox().isEmpty(); // true

        System.out.println(
                "Q16 received: " + receivedByBob + ", senderEmpty: " + senderNotSelfNotified);
    }
}
