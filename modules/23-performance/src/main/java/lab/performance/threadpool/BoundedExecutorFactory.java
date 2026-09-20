package lab.performance.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class BoundedExecutorFactory {
    public ThreadPoolExecutor create(WorkloadProfile profile) {
        int workers = profile.workerCount();
        return new ThreadPoolExecutor(
                workers,
                workers,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(profile.queueCapacity()),
                // Rejection exposes overload to the caller instead of hiding it in queue growth.
                new ThreadPoolExecutor.AbortPolicy());
    }
}
