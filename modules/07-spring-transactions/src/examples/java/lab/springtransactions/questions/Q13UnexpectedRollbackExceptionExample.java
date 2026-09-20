package lab.springtransactions.questions;

import org.springframework.transaction.UnexpectedRollbackException;

public class Q13UnexpectedRollbackExceptionExample {

    record PropagationFailure(
            String outerPropagation,
            String innerPropagation,
            boolean innerMarkedRollbackOnly,
            boolean exceptionThrownOnOuterCommit) {}

    public static void main(String[] args) {
        // When inner REQUIRED transaction fails, it marks physical transaction rollback-only.
        // If outer transaction catches exception and attempts to commit, Spring throws
        // UnexpectedRollbackException.
        PropagationFailure failure = new PropagationFailure("REQUIRED", "REQUIRED", true, true);

        boolean isMarkedOnly = failure.innerMarkedRollbackOnly(); // true
        boolean throwsOnCommit = failure.exceptionThrownOnOuterCommit(); // true

        System.out.println(
                "Rollback-only marked: "
                        + isMarkedOnly
                        + ", throws on commit: "
                        + throwsOnCommit
                        + ", ex: "
                        + UnexpectedRollbackException.class.getSimpleName());
    }
}
