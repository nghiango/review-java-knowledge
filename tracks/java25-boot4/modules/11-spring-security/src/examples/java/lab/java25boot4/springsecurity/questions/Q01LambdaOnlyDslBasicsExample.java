package lab.java25boot4.springsecurity.questions;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Q01: How does the lambda-only DSL in Spring Security 7 replace legacy method chaining and improve
 * configuration safety?
 */
public class Q01LambdaOnlyDslBasicsExample {

    public static void main(String[] args) {
        // In Spring Security 7, all configuration methods strictly require a Customizer lambda
        Customizer<String> customizer = s -> System.out.println("Configuring: " + s);

        boolean isFunctional = (customizer != null);

        System.out.println("Customizer functional: " + isFunctional); // true
        System.out.println("Config class: " + HttpSecurity.class.getSimpleName()); // "HttpSecurity"
    }
}
