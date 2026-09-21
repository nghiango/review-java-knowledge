package lab.java25boot4.springtransactions;

import java.util.concurrent.StructuredTaskScope;
import org.jspecify.annotations.Nullable;

/**
 * Demonstrates combining ScopedValue with transactional metadata propagation. Context is immutable
 * and automatically inherited by child virtual threads in a StructuredTaskScope.
 */
public class ScopedTransactionContextCoordinator {

    public record TxContext(String txId, String tenantId, long timestamp) {}

    public static final ScopedValue<TxContext> CURRENT_TX = ScopedValue.newInstance();

    public static <T, X extends Throwable> T runInContext(
            TxContext ctx, ScopedValue.CallableOp<T, X> action) throws X {
        return ScopedValue.where(CURRENT_TX, ctx).call(action);
    }

    public static @Nullable TxContext getCurrentContext() {
        if (CURRENT_TX.isBound()) {
            return CURRENT_TX.get();
        }
        return null;
    }

    public static String executeWithForkedVirtualWorkers(TxContext ctx) throws Exception {
        return ScopedValue.where(CURRENT_TX, ctx)
                .call(
                        () -> {
                            try (var scope = StructuredTaskScope.open()) {
                                var task1 =
                                        scope.fork(
                                                () -> {
                                                    var current = getCurrentContext();
                                                    return current != null
                                                            ? current.txId()
                                                            : "NONE";
                                                });

                                var task2 =
                                        scope.fork(
                                                () -> {
                                                    var current = getCurrentContext();
                                                    return current != null
                                                            ? current.tenantId()
                                                            : "NONE";
                                                });

                                scope.join();
                                return task1.get() + ":" + task2.get();
                            }
                        });
    }
}
