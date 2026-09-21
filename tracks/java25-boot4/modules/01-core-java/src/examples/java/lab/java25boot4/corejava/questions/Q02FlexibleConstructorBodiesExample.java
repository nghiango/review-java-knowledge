package lab.java25boot4.corejava.questions;

import java.util.Objects;

public class Q02FlexibleConstructorBodiesExample {

    static class SuperClass {
        final String tag;

        SuperClass(String tag) {
            this.tag = Objects.requireNonNull(tag, "tag must not be null");
        }
    }

    static class SubClass extends SuperClass {
        final int port;

        SubClass(String rawTag, int port) {
            // Flexible constructor body: statements before super(...)
            String sanitizedTag = rawTag.trim().toLowerCase();
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("Port must be non-privileged: " + port);
            }

            super(sanitizedTag); // Invoke super constructor with validated arguments

            this.port = port;
        }
    }

    public static void main(String[] args) {
        var sub = new SubClass("  PROD-API  ", 8080);
        System.out.println(sub.tag); // "prod-api"
        System.out.println(sub.port); // 8080
    }
}
