package lab.springtransactions.asynctransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

    private final List<String> dispatchedEmails = Collections.synchronizedList(new ArrayList<>());

    // @TransactionalEventListener guarantees execution ONLY after the database transaction
    // successfully commits
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        dispatchedEmails.add(event.customerEmail() + ":" + event.orderId());
    }

    public List<String> getDispatchedEmails() {
        return List.copyOf(dispatchedEmails);
    }
}
