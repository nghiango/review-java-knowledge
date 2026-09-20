package lab.jpahibernate.questions;

public class Q21ConcurrentInventoryReservation {

    public static void main(String[] args) {
        // High-contention flash sale vs low-contention checkout:
        // Low/Medium contention: Optimistic locking (@Version) + retry backoff (low DB lock
        // overhead).
        // High contention: Pessimistic locking (SELECT FOR UPDATE) or atomic single-statement
        // UPDATE:
        // "UPDATE Inventory i SET i.stock = i.stock - :qty WHERE i.id = :id AND i.stock >= :qty"
        boolean atomicSqlUpdateEliminatesLockingOverhead = true; // true
        int initialStock = 100; // 100
        int decrementQty = 5; // 5
        int remainingStock = initialStock - decrementQty; // 95

        System.out.println(
                "Atomic SQL update eliminates ORM locking: "
                        + atomicSqlUpdateEliminatesLockingOverhead); // Atomic SQL update eliminates
        // ORM locking: true
        System.out.println("Remaining stock: " + remainingStock); // Remaining stock: 95
    }
}
