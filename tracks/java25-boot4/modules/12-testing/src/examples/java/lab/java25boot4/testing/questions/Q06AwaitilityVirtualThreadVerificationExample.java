package lab.java25boot4.testing.questions;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.awaitility.Awaitility;

/**
 * Q06: How does Awaitility facilitate poll-based asynchronous state verification without flaky
 * timeouts?
 */
public class Q06AwaitilityVirtualThreadVerificationExample {

    public static void main(String[] args) {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

        Thread.startVirtualThread(
                () -> {
                    try {
                        Thread.sleep(30);
                        queue.add("item1");
                        queue.add("item2");
                    } catch (InterruptedException ignored) {
                    }
                });

        Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> queue.size() >= 2);

        System.out.println("Queue items collected: " + queue.size()); // Queue items collected: 2
    }
}
