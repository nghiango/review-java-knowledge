package lab.jpahibernate.questions;

import jakarta.persistence.LockModeType;

public class Q15OptimisticVsPessimisticLocking {

    public static void main(String[] args) {
        // Optimistic locking uses @Version (number or timestamp); throws OptimisticLockException on
        // concurrent conflict upon commit.
        boolean optimisticLockAcquiresDbRowLock =
                false; // false (checked at UPDATE WHERE version = ?)

        // Pessimistic locking uses SQL "SELECT ... FOR UPDATE" (PESSIMISTIC_WRITE) or shared lock
        // (PESSIMISTIC_READ).
        LockModeType pessimisticWrite = LockModeType.PESSIMISTIC_WRITE;
        boolean pessimisticWriteAcquiresDbRowLock =
                (pessimisticWrite == LockModeType.PESSIMISTIC_WRITE); // true

        System.out.println(
                "Optimistic acquires DB row lock: "
                        + optimisticLockAcquiresDbRowLock); // Optimistic acquires DB row lock:
        // false
        System.out.println(
                "PESSIMISTIC_WRITE acquires DB row lock: "
                        + pessimisticWriteAcquiresDbRowLock); // PESSIMISTIC_WRITE acquires DB row
        // lock: true
    }
}
