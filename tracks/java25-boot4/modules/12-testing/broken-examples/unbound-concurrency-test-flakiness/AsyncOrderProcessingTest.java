package lab.java25boot4.testing.broken.asyncflakiness;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AsyncOrderProcessingTest {

    private static final List<String> NOTIFICATIONS_LOG = new ArrayList<>();

    @Test
    public void testOrderNotificationDispatched() throws InterruptedException {
        OrderNotificationService service = new OrderNotificationService();

        service.dispatchNotification("ORD-8899", status -> NOTIFICATIONS_LOG.add(status));

        // Attempting to wait for async virtual thread completion using arbitrary sleep
        Thread.sleep(50);

        assertThat(NOTIFICATIONS_LOG).contains("NOTIFIED:ORD-8899");
    }
}
