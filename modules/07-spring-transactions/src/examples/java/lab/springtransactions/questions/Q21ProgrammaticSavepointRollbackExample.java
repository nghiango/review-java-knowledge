package lab.springtransactions.questions;

import java.util.ArrayList;
import java.util.List;

public class Q21ProgrammaticSavepointRollbackExample {

    record AccountBalance(String accountId, double balance) {}

    public static void main(String[] args) {
        // Programmatic savepoints allow rolling back partial operations without aborting the parent
        // transaction
        List<AccountBalance> entries = new ArrayList<>();
        entries.add(new AccountBalance("acc-1", 100.0));

        // Savepoint created before optional operation
        int savepointIndex = entries.size(); // 1

        // Optional operation fails
        boolean optionalOpFailed = true;
        if (optionalOpFailed) {
            // Rollback to savepoint: remove items added after savepointIndex
            while (entries.size() > savepointIndex) {
                entries.remove(entries.size() - 1);
            }
        }

        boolean primaryPreserved = entries.size() == 1; // true
        double balance = entries.get(0).balance(); // 100.0

        System.out.println(
                "Primary operation preserved: " + primaryPreserved + ", balance: " + balance);
    }
}
