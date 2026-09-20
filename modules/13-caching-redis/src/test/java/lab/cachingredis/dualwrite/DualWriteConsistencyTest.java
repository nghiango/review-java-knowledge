package lab.cachingredis.dualwrite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class DualWriteConsistencyTest {

    private SafeWalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new SafeWalletService();
    }

    @Test
    @DisplayName("Successful transaction evicts cache and subsequent read fetches fresh state")
    void successfulTransfer_evictsCacheAndReloadsFreshState() {
        assertThat(walletService.getBalance("acc-101").balance()).isEqualByComparingTo("1000.00");
        assertThat(walletService.getBalance("acc-102").balance()).isEqualByComparingTo("500.00");

        // Execute transfer
        walletService.transferFunds("acc-101", "acc-102", new BigDecimal("200.00"));

        // Cache entries for acc-101 and acc-102 were evicted
        assertThat(walletService.getCacheState().containsKey("wallet:acc-101")).isFalse();
        assertThat(walletService.getCacheState().containsKey("wallet:acc-102")).isFalse();

        // Reading balance again pulls fresh state from DB
        WalletBalance reloadedSender = walletService.getBalance("acc-101");
        WalletBalance reloadedReceiver = walletService.getBalance("acc-102");

        assertThat(reloadedSender.balance()).isEqualByComparingTo("800.00");
        assertThat(reloadedReceiver.balance()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Rolled back transaction does not contaminate cache with uncommitted data")
    void rolledBackTransaction_doesNotContaminateCache() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            // Attempting transfer of 50,000 exceeds fraud threshold and triggers exception
            assertThatThrownBy(
                            () ->
                                    walletService.transferFunds(
                                            "acc-101", "acc-102", new BigDecimal("50000.00")))
                    .isInstanceOf(IllegalStateException.class);

            // Because transaction failed, afterCommit never ran and cache was not polluted with
            // phantom data
            WalletBalance senderCached = walletService.getCacheState().get("wallet:acc-101");
            assertThat(senderCached.balance()).isEqualByComparingTo("1000.00");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
