package lab.observability.questions;

public class Q05StructuredJsonLoggingExample {

    record LogFormat(String name, boolean machineIndexable, boolean requiresComplexRegexParsing) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Human-readable plaintext logs require fragile regex patterns in log parsers (Logstash /
        // FluentBit).
        // Structured JSON logs serialize timestamp, level, message, MDC attributes, and exception
        // stacks
        // as first-class JSON keys, enabling fast, zero-loss indexing in Elasticsearch, Datadog, or
        // ClickHouse.
        LogFormat text = new LogFormat("Plaintext / PatternLayout", false, true);
        LogFormat json = new LogFormat("Logstash JSON Encoder", true, false);

        boolean jsonIsIndexable = json.machineIndexable(); // true
        boolean textRequiresRegex = text.requiresComplexRegexParsing(); // true

        System.out.println("JSON logging is machine indexable: " + jsonIsIndexable);
        System.out.println("Plaintext logs require complex regex: " + textRequiresRegex);
    }
}
