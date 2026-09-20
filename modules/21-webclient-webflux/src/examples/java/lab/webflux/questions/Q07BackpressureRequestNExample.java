package lab.webflux.questions;

import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

public class Q07BackpressureRequestNExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Backpressure allows a downstream subscriber to signal its capacity using
        // Subscription.request(n).
        // The publisher will only emit up to n elements until the subscriber issues another
        // request(n).
        List<Integer> received = new ArrayList<>();

        Flux.range(1, 100)
                .subscribe(
                        new Subscriber<Integer>() {
                            private Subscription subscription;

                            @Override
                            public void onSubscribe(Subscription s) {
                                this.subscription = s;
                                // Request exactly 2 elements initially
                                s.request(2);
                            }

                            @Override
                            public void onNext(Integer integer) {
                                received.add(integer);
                            }

                            @Override
                            public void onError(Throwable t) {}

                            @Override
                            public void onComplete() {}
                        });

        int countReceived = received.size(); // 2
        boolean respectsDemand = countReceived == 2; // true

        System.out.println("Items received respecting backpressure demand: " + countReceived);
    }
}
