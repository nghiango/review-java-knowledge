package lab.springboot.gracefulshutdown;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class GracefulTaskProcessor {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public GracefulTaskProcessor(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void submitJob(Runnable task) {
        taskExecutor.submit(
                () -> {
                    task.run();
                    processedCount.incrementAndGet();
                });
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public ThreadPoolTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }
}
