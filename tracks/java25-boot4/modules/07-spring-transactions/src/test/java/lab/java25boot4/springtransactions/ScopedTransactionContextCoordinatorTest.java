package lab.java25boot4.springtransactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScopedTransactionContextCoordinatorTest {

    @Test
    @DisplayName("Should inherit ScopedValue transaction context across forked virtual threads")
    void executeWithForkedVirtualWorkers_inheritsContext() throws Exception {
        var ctx =
                new ScopedTransactionContextCoordinator.TxContext(
                        "tx-999", "tenant-alpha", 123456789L);
        String result = ScopedTransactionContextCoordinator.executeWithForkedVirtualWorkers(ctx);

        assertThat(result).isEqualTo("tx-999:tenant-alpha");
    }

    @Test
    @DisplayName("Should return null outside of bound scope")
    void getCurrentContext_unbound_returnsNull() {
        assertThat(ScopedTransactionContextCoordinator.getCurrentContext()).isNull();
    }
}
