package lab.concurrency.questions;

import java.util.concurrent.atomic.AtomicReference;

/** Q21: Demonstrates safe publication patterns (final fields, volatile, atomic reference). */
@SuppressWarnings("unused")
public class Q21SafePublicationPatternsExample {

    // 1. Safe publication via immutable state with final fields
    public static final class ImmutableConfig {
        private final String host;
        private final int port;

        public ImmutableConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    // 2. Safe publication via volatile reference
    private static volatile ImmutableConfig volatileConfig;

    // 3. Safe publication via AtomicReference
    private static final AtomicReference<ImmutableConfig> atomicConfig =
            new AtomicReference<>(new ImmutableConfig("localhost", 8080));

    public static void main(String[] args) {
        volatileConfig = new ImmutableConfig("db.production.internal", 5432);

        String host = volatileConfig.getHost(); // "db.production.internal"
        int port = atomicConfig.get().getPort(); // 8080
    }
}
