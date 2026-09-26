package lab.observability.questions;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demonstrates an incident where asynchronous tasks (@Async / CompletableFuture / thread pool dispatch)
 * dropped MDC and distributed trace context because ThreadLocals were not propagated to worker threads.
 *
 * <p>Fix: Wrap the ExecutorService with Context-propagating decorators (e.g. {@code ContextExecutorService}
 * or TaskDecorator in Spring) to capture and restore trace state across thread boundaries.</p>
 */
public class Q30IncidentAsyncContextLossTraceDropExample {

    public static void main(String[] args) throws Exception {
        ExecutorService rawExecutor = Executors.newSingleThreadExecutor();
        AtomicReference<String> workerTraceIdWithoutPropagation = new AtomicReference<>();
        AtomicReference<String> workerTraceIdWithPropagation = new AtomicReference<>();

        String expectedTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        MDC.put("traceId", expectedTraceId);

        // Buggy dispatch: ThreadLocal MDC is empty on new thread
        CompletableFuture.runAsync(() -> {
            workerTraceIdWithoutPropagation.set(MDC.get("traceId")); // null
        }, rawExecutor).get();

        // Correct dispatch: Propagate MDC snapshot into task
        final String capturedTraceId = MDC.get("traceId");
        CompletableFuture.runAsync(() -> {
            MDC.put("traceId", capturedTraceId);
            try {
                workerTraceIdWithPropagation.set(MDC.get("traceId"));
            } finally {
                MDC.clear();
            }
        }, rawExecutor).get();

        rawExecutor.shutdown();

        boolean traceLostOnRawAsync = workerTraceIdWithoutPropagation.get() == null; // true
        boolean tracePreservedWithWrapper = expectedTraceId.equals(workerTraceIdWithPropagation.get()); // true

        System.out.println("Trace context lost on raw async: " + traceLostOnRawAsync);
        System.out.println("Trace context preserved with decorator: " + tracePreservedWithWrapper);
    }
}
