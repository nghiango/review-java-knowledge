package lab.rabbitmq.questions;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

/**
 * Q15: How does Jackson2JsonMessageConverter serialize AMQP messages, and how does __TypeId__
 * mapping work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q15SpringAmqpMessageConverters {

    public static void main(String[] args) {
        // Default Spring AMQP converter uses Java serialization (SimpleMessageConverter),
        // which introduces security vulnerabilities and cross-language interoperability barriers.
        // Jackson2JsonMessageConverter standardizes payloads to UTF-8 JSON.
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        boolean jsonStandardActive = (converter != null); // true

        // Type Headers:
        // By default, publisher sets __TypeId__ in basic properties to the fully qualified class
        // name.
        // If consumer has a different package name or refactored class, deserialization fails!
        // Solution: Configure DefaultClassMapper with setTrustedPackages("*") and
        // setTypePrecedence(TYPE_ID)
        // or configure DefaultJackson2JavaTypeMapper with custom idClassMapping map.
        String typeIdHeader = "__TypeId__";
        boolean usesCustomClassMapping = true; // true
    }
}
