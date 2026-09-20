package lab.observability.questions;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.MDC;

public class Q14MdcContextTaskDecoratorExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // TaskDecorator pattern: wraps asynchronous runnables to propagate and clean up MDC context
        ExecutorService pool = Executors.newSingleThreadExecutor();

        MDC.put("correlationId", "req-xyz-123");
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        AtomicReference<String> workerCorrelation = new AtomicReference<>();

        Runnable decoratedTask =
                () -> {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    try {
                        workerCorrelation.set(MDC.get("correlationId"));
                    } finally {
                        MDC.clear();
                    }
                };

        pool.submit(decoratedTask).get();
        pool.shutdown();

        boolean correlationPropagated = "req-xyz-123".equals(workerCorrelation.get()); // true
        MDC.clear();

        System.out.println(
                "MDC correlation ID propagated to worker thread: " + correlationPropagated);
    }
}
