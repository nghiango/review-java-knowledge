package lab.springtransactions.broken.checkedexception;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementService {

    @Transactional
    public void placeOrder(String orderId, double amount) throws OrderValidationException {
        // Step 1: Save initial order record
        insertOrder(orderId, amount);

        // Step 2: Checked validation failure
        if (amount <= 0) {
            // Checked exception thrown: Spring default rollback does NOT roll back checked exceptions!
            throw new OrderValidationException("Invalid order amount: " + amount);
        }

        updateStatus(orderId, "CONFIRMED");
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }

    private void updateStatus(String orderId, String status) {
        // DB update
    }
}
