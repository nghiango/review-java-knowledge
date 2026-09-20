package lab.webflux.questions;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Q06SubscribeOnVsPublishOnExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // .subscribeOn() changes the execution thread of the upstream subscription and source
        // generation.
        // .publishOn() changes the execution thread of downstream operators following the
        // publishOn() call.
        AtomicReference<String> sourceThread = new AtomicReference<>();
        AtomicReference<String> downstreamThread = new AtomicReference<>();

        Mono.fromCallable(
                        () -> {
                            sourceThread.set(Thread.currentThread().getName());
                            return "payload";
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .map(
                        val -> {
                            downstreamThread.set(Thread.currentThread().getName());
                            return val.toUpperCase(Locale.ROOT);
                        })
                .block();

        boolean sourceIsBoundedElastic = sourceThread.get().contains("boundedElastic"); // true
        boolean downstreamIsParallel = downstreamThread.get().contains("parallel"); // true

        System.out.println("Source ran on boundedElastic: " + sourceIsBoundedElastic);
        System.out.println("Downstream ran on parallel: " + downstreamIsParallel);
    }
}
