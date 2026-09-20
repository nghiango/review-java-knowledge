package lab.springboot.broken.gracefulshutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class BatchTaskProcessor {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public void submitJob(Runnable task) {
        executor.submit(
                () -> {
                    task.run();
                    processedCount.incrementAndGet();
                });
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
