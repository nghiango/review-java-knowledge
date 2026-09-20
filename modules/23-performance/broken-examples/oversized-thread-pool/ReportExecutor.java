package lab.performance.broken.threadpool;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ReportExecutor implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(500);

    public List<Future<byte[]>> render(List<ReportJob> jobs) {
        return jobs.stream().map(job -> executor.submit(job::render)).toList();
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    public interface ReportJob {
        byte[] render();
    }
}
