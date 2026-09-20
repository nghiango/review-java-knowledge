package lab.observability.correlationasync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrectAsyncOrderAuditServiceTest {

    private ExecutorService executor;
    private CorrectAsyncOrderAuditService service;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        service = new CorrectAsyncOrderAuditService(executor);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        MDC.clear();
    }

    @Test
    @DisplayName("Should capture MDC context map and clear it on worker thread completion")
    void auditOrderPlacement_propagatesAndClearsMdc() throws Exception {
        CompletableFuture<Void> future = service.auditOrderPlacement("order-456", "user-789");
        future.get(3, TimeUnit.SECONDS);

        // Verify parent thread still has its context or handles it cleanly
        assertThat(MDC.get("correlationId")).isEqualTo("corr-order-456");

        // Verify background thread was cleared after task completion
        AtomicReference<String> workerMdc = new AtomicReference<>();
        executor.submit(() -> workerMdc.set(MDC.get("correlationId"))).get(1, TimeUnit.SECONDS);
        assertThat(workerMdc.get()).isNull();
    }
}
