package lab.java25boot4.corejava;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VirtualThreadSynchronizedSafetyTest {

    @Test
    @DisplayName(
            "Virtual threads execute synchronized work safely without carrier thread starvation")
    void shouldExecuteSynchronizedVirtualThreadsSafely() throws InterruptedException {
        var safety = new VirtualThreadSynchronizedSafety();
        int taskCount = 50;
        var latch = new CountDownLatch(taskCount);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(
                        () -> {
                            try {
                                safety.executeSynchronizedWork(latch);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
            }
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(safety.getCompletedOperations()).isEqualTo(taskCount);
    }
}
