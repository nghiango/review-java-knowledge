package lab.concurrency.unboundedthreads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating production-ready bounded thread pools with descriptive thread naming,
 * uncaught exception handling, and configurable backpressure policies.
 */
public final class CustomThreadPoolFactory {

    private CustomThreadPoolFactory() {}

    public static ThreadPoolExecutor createBoundedPool(
            String poolName,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            RejectedExecutionHandler rejectionHandler) {

        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);
        ThreadFactory threadFactory = new NamedThreadFactory(poolName);

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectionHandler != null
                        ? rejectionHandler
                        : new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static final class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final Thread.UncaughtExceptionHandler exceptionHandler;

        public NamedThreadFactory(String namePrefix) {
            this(
                    namePrefix,
                    (t, e) ->
                            System.err.printf(
                                    "[Thread %s] Uncaught exception: %s%n",
                                    t.getName(), e.getMessage()));
        }

        public NamedThreadFactory(
                String namePrefix, Thread.UncaughtExceptionHandler exceptionHandler) {
            this.namePrefix = namePrefix;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        }
    }
}
