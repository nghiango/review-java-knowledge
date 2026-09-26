package lab.jpahibernate.questions;

@SuppressWarnings("unused")
public final class Q30InventoryOversellingScenarioExample {
    private Q30InventoryOversellingScenarioExample() {}

    public static class InventoryItem {
        private int stock = 1;
        private int version = 0;

        // Solution 1: Atomic database condition (atomic check-and-decrement in SQL)
        // UPDATE item SET stock = stock - :qty WHERE id = :id AND stock >= :qty
        public synchronized boolean decrementAtomic(int quantity) {
            if (this.stock >= quantity) {
                this.stock -= quantity;
                this.version++;
                return true;
            }
            return false;
        }

        // Solution 2: Optimistic Locking simulation (@Version)
        public synchronized void updateWithVersion(int quantity, int readVersion) {
            if (readVersion != this.version) {
                throw new RuntimeException(
                        "OptimisticLockException: Row was updated or deleted by another transaction");
            }
            if (this.stock < quantity) {
                throw new IllegalStateException("Insufficient inventory");
            }
            this.stock -= quantity;
            this.version++;
        }

        public int getStock() {
            return stock;
        }
    }

    public static void main(String[] args) {
        InventoryItem item = new InventoryItem();

        // Customer A and Customer B both read stock = 1 simultaneously:
        boolean customerA = item.decrementAtomic(1); // true (stock reserved, stock becomes 0)
        boolean customerB = item.decrementAtomic(1); // false (prevented overselling!)

        int remainingStock = item.getStock(); // 0 (overselling prevented)
    }
}
