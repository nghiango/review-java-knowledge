package lab.concurrency.volatilecompound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("FutureReturnValueIgnored")
class InventoryReservationServiceTest {

    @Test
    @DisplayName("concurrent flash-sale reservations never oversell and stock never goes negative")
    void concurrentFlashSale_neverOversells() throws InterruptedException {
        InventoryReservationService service = new InventoryReservationService();
        String itemId = "ITEM-FLASH-01";
        int totalStock = 100;
        service.registerItem(itemId, totalStock);

        int buyers = 50;
        int itemsPerBuyer =
                3; // 50 * 3 = 150 requested, but only 100 available (33 buyers succeed, 17 fail)

        ExecutorService executor = Executors.newFixedThreadPool(buyers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(buyers);
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        for (int i = 0; i < buyers; i++) {
            executor.submit(
                    () -> {
                        try {
                            startGate.await();
                            boolean ok = service.reserve(itemId, itemsPerBuyer);
                            if (ok) {
                                successfulReservations.incrementAndGet();
                            } else {
                                failedReservations.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endGate.countDown();
                        }
                    });
        }

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        int expectedSuccess = totalStock / itemsPerBuyer; // 33
        int expectedRemaining = totalStock % itemsPerBuyer; // 1

        assertThat(successfulReservations.get()).isEqualTo(expectedSuccess);
        assertThat(failedReservations.get()).isEqualTo(buyers - expectedSuccess);
        assertThat(service.getAvailableStock(itemId)).isEqualTo(expectedRemaining);
    }
}
