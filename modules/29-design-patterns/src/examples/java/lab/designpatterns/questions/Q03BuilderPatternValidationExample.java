package lab.designpatterns.questions;

import java.util.Objects;

/**
 * Q03: Builder Pattern with Invariant Validation. Demonstrates constructing immutable objects with
 * multi-attribute validation in build().
 */
public class Q03BuilderPatternValidationExample {

    public static class DatabaseConfig {
        private final String host;
        private final int port;
        private final int poolSize;

        private DatabaseConfig(Builder builder) {
            this.host = builder.host;
            this.port = builder.port;
            this.poolSize = builder.poolSize;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public static class Builder {
            private String host;
            private int port = 5432;
            private int poolSize = 10;

            public Builder host(String host) {
                this.host = host;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder poolSize(int poolSize) {
                this.poolSize = poolSize;
                return this;
            }

            public DatabaseConfig build() {
                Objects.requireNonNull(host, "Database host cannot be null");
                if (port <= 0 || port > 65535) {
                    throw new IllegalArgumentException("Invalid port: " + port);
                }
                if (poolSize <= 0) {
                    throw new IllegalArgumentException("Pool size must be positive");
                }
                return new DatabaseConfig(this);
            }
        }
    }

    public static void main(String[] args) {
        DatabaseConfig config =
                new DatabaseConfig.Builder().host("localhost").port(5432).poolSize(20).build();

        boolean validConfig =
                config.getHost().equals("localhost") && config.getPoolSize() == 20; // true

        System.out.println("Q03 builderConfigured: " + validConfig);
    }
}
