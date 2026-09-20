package lab.webflux.questions;

public class Q02ReactiveStreamsInterfacesExample {

    record ReactiveRole(String interfaceName, String responsibility) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Reactive Streams defines 4 fundamental interfaces:
        // 1. Publisher: Emits data items to registered Subscribers.
        // 2. Subscriber: Receives onSubscribe, onNext, onError, onComplete signals.
        // 3. Subscription: Connects Publisher and Subscriber, enabling backpressure via request(n)
        // and cancel().
        // 4. Processor: Acts as both a Subscriber and a Publisher (pipeline stage).
        ReactiveRole pub = new ReactiveRole("Publisher", "Produces data stream");
        ReactiveRole sub = new ReactiveRole("Subscriber", "Consumes data items");
        ReactiveRole subscr =
                new ReactiveRole("Subscription", "Manages backpressure via request(n)");
        ReactiveRole proc = new ReactiveRole("Processor", "Transforms and relays stream");

        int coreInterfacesCount = 4; // 4
        boolean backpressureEnforcedBySubscription =
                subscr.responsibility().contains("request(n)"); // true

        System.out.println(
                "Reactive Streams specification core interfaces count: " + coreInterfacesCount);
    }
}
