package lab.webflux.questions;

import java.util.Map;

/**
 * Q28: What is the difference between Cold and Hot publishers, and how do connectable flows prevent duplicate work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q28HotVsColdPublishersRefCountExample {

    public static void main(String[] args) {
        // Cold Publisher (Default):
        // Generates new data sequence independently for each subscriber.
        // E.g. Flux.just(), webClient.get().retrieve().bodyToFlux(): Each subscribe() fires a new HTTP call!
        //
        // Hot Publisher:
        // Emits data regardless of whether subscribers are listening. Subscribers receive only events
        // emitted AFTER they subscribe (or replayed via Sinks).
        //
        // Turning Cold into Hot - publish().refCount(1) / share():
        // Multiplexes a single underlying HTTP or Kafka stream across multiple subscribers.
        // When the first subscriber joins, it subscribes upstream; subsequent subscribers share the stream;
        // when all subscribers cancel, it automatically cancels the upstream subscription!

        Map<String, String> publisherTypes =
                Map.of(
                        "Cold", "Independent execution per subscriber (e.g. fresh HTTP query on each subscribe)",
                        "Hot / Multicast", "Shared single execution; share() / publish().refCount(1) prevents duplicate calls");

        boolean refCountSharesSingleUpstream =
                publisherTypes.get("Hot / Multicast").contains("prevents duplicate calls"); // true
    }
}
