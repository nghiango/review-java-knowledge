package lab.java25boot4.testing.broken.asyncflakiness;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class OrderNotificationService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void dispatchNotification(String orderId, Consumer<String> onComplete) {
        executor.submit(() -> {
            try {
                // Simulate network latency for external notification
                Thread.sleep(80);
                onComplete.accept("NOTIFIED:" + orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
