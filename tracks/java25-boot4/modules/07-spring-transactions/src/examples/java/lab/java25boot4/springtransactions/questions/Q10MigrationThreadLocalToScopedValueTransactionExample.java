package lab.java25boot4.springtransactions.questions;

import java.util.concurrent.StructuredTaskScope;

public class Q10MigrationThreadLocalToScopedValueTransactionExample {

    private static final ScopedValue<String> TX_ID = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        // Replacing ThreadLocal transaction metadata with ScopedValue
        ScopedValue.where(TX_ID, "tx-9999")
                .run(
                        () -> {
                            try (var scope = StructuredTaskScope.open()) {
                                var task = scope.fork(() -> "Worker inherited: " + TX_ID.get());
                                scope.join();
                                System.out.println(task.get()); // Worker inherited: tx-9999
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
    }
}
