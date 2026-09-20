package lab.concurrency.volatilecompound;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe inventory item tracking stock with an atomic compare-and-swap CAS state machine. */
public class ItemStock {
    private final String itemId;
    private final AtomicInteger availableQuantity;

    public ItemStock(String itemId, int initialQuantity) {
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative");
        }
        this.availableQuantity = new AtomicInteger(initialQuantity);
    }

    public String getItemId() {
        return itemId;
    }

    public int getAvailableQuantity() {
        return availableQuantity.get();
    }

    /**
     * Atomically reserves the requested quantity using a lock-free CAS loop.
     *
     * @param quantity amount to reserve
     * @return true if stock was atomically decremented; false if insufficient stock
     */
    public boolean tryReserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
        return availableQuantity.updateAndGet(
                                current -> current >= quantity ? current - quantity : current)
                        >= 0
                && availableQuantity.get() >= 0;
    }

    /** Exact atomic decrement that ensures reservation only succeeds if remaining >= 0. */
    public boolean reserveExact(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
        while (true) {
            int current = availableQuantity.get();
            if (current < quantity) {
                return false;
            }
            int updated = current - quantity;
            if (availableQuantity.compareAndSet(current, updated)) {
                return true;
            }
        }
    }

    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive");
        }
        availableQuantity.addAndGet(quantity);
    }
}
