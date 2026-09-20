package lab.springcore.mutablesingleton;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("FutureReturnValueIgnored")
class DiscountCalculationServiceTest {

    @Test
    @DisplayName(
            "stateless singleton calculation produces deterministic results under concurrent execution")
    void concurrentCalculations_maintainIsolation() throws InterruptedException {
        DiscountCalculationService service = new DiscountCalculationService();
        int threads = 16;
        int runsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger correctResults = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int threadIdx = i;
            executor.submit(
                    () -> {
                        try {
                            startGate.await();
                            for (int j = 0; j < runsPerThread; j++) {
                                if (threadIdx % 2 == 0) {
                                    DiscountResult res =
                                            service.calculateDiscount(
                                                    new DiscountRequest("CUST-HIGH", 1000.0, 2000));
                                    if (res.totalDiscount() == 120.0
                                            && "CUST-HIGH".equals(res.customerId())) {
                                        correctResults.incrementAndGet();
                                    }
                                } else {
                                    DiscountResult res =
                                            service.calculateDiscount(
                                                    new DiscountRequest("CUST-LOW", 100.0, 100));
                                    if (res.totalDiscount() == 10.0
                                            && "CUST-LOW".equals(res.customerId())) {
                                        correctResults.incrementAndGet();
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endGate.countDown();
                        }
                    });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(correctResults.get()).isEqualTo(threads * runsPerThread);
    }
}
