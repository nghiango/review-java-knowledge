package lab.rabbitmq.questions;

import java.util.Map;

/** Q02: What are the routing semantics of Direct, Fanout, Topic, and Headers exchanges? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q02ExchangeTypesRoutingSemantics {

    public static void main(String[] args) {
        // Direct Exchange: Exact match between message routing key and binding key
        String directBinding = "orders.created";
        boolean directMatchesExact = "orders.created".equals(directBinding); // true

        // Fanout Exchange: Broadcasts messages to all bound queues unconditionally (ignores routing
        // key)
        boolean fanoutIgnoresRoutingKey = true; // true

        // Topic Exchange: Pattern-based routing with wildcards:
        // * (star) matches exactly one word
        // # (hash) matches zero or more words
        Map<String, String> topicWildcards =
                Map.of(
                        "audit.*",
                                "matches audit.login, audit.logout; does NOT match audit.security.auth",
                        "audit.#", "matches audit.login, audit.security.auth, audit.a.b.c");

        boolean hashMatchesMultipleWords =
                topicWildcards.get("audit.#").contains("audit.security.auth"); // true

        // Headers Exchange: Routes based on message header key-value pairs (x-match: all or any)
        boolean headersIgnoresRoutingKey = true; // true
    }
}
