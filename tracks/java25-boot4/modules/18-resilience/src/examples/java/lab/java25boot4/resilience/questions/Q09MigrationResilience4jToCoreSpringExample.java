package lab.java25boot4.resilience.questions;

import java.time.Duration;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q09: How do you migrate heavy external Resilience4j decorators to modern Spring Boot 4
 * lightweight execution pipelines?
 */
public class Q09MigrationResilience4jToCoreSpringExample {

    public static void main(String[] args) {
        // Modern Spring Boot 4 composition: composable lightweight engine without AOP bytecode
        // weaving
        var retryConfig = ModernResilientExecutionEngine.RetryConfig.defaultTransient(3);
        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(5, Duration.ofSeconds(1));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 50);

        String result = engine.executeWithResilience(() -> "MIGRATED_PIPELINE_SUCCESS");

        System.out.println(
                "Execution engine active: " + (engine != null)); // Execution engine active: true
        System.out.println(
                "Execution output: " + result); // Execution output: MIGRATED_PIPELINE_SUCCESS
    }
}
