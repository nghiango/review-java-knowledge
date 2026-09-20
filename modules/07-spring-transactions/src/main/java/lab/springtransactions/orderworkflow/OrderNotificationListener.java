package lab.springtransactions.orderworkflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

    private final List<OrderCompletedEvent> notifications =
            Collections.synchronizedList(new ArrayList<>());

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        notifications.add(event);
    }

    public List<OrderCompletedEvent> getNotifications() {
        return List.copyOf(notifications);
    }
}
