package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q06: What serialization mechanisms are available for Redis in Spring Data Redis, and why is JDK
 * serialization avoided?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q06RedisSerializationStrategies {

    public static void main(String[] args) {
        // Serialization strategies:
        // 1. JdkSerializationRedisSerializer (Default in old Spring Redis):
        //    - Flaws: Extremely verbose binary footprint; brittle across classloader refactors;
        //      severe remote code execution (RCE) vulnerability during deserialization of untrusted
        // payloads.
        boolean jdkSerializationDangerous = true; // true

        // 2. StringRedisSerializer:
        //    - Encodes strings as raw UTF-8 bytes. Zero overhead, perfectly portable across
        // languages.
        boolean stringSerializerPortable = true; // true

        // 3. GenericJackson2JsonRedisSerializer:
        //    - Stores "@class" metadata in JSON to deserialize polymorphically.
        //    - Flaw: Leaks Java internal package structures into Redis; risk of deserialization
        // gadget exploits if unvalidated.
        boolean jacksonGenericStoresClassProperty = true; // true

        // 4. Jackson2JsonRedisSerializer<T> (Targeted DTO):
        //    - Serializes specific POJO/record directly to clean JSON without @class meta.
        //    - Best production practice for cross-language microservice architectures.
        boolean typedDtoBestPractice = true; // true

        Map<String, String> summary =
                Map.of(
                        "JDK", "Insecure and fragile",
                        "String", "Lightweight and human readable",
                        "TypedJackson", "Clean schema without class injection");

        boolean avoidJdkInProduction = summary.get("JDK").contains("Insecure"); // true
    }
}
