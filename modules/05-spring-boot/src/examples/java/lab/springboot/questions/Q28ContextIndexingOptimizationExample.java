package lab.springboot.questions;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@SuppressWarnings("unused")
public final class Q28ContextIndexingOptimizationExample {
    private Q28ContextIndexingOptimizationExample() {}

    // When org.springframework:spring-context-indexer is present as an annotation processor:
    // Any class annotated with @Component, @Service, @Repository, @Controller
    // is recorded at compile time into META-INF/spring.components.
    @Service
    public static class HighThroughputBillingService {
        public String executeBilling() {
            return "billed";
        }
    }

    public static void main(String[] args) {
        // Standard classpath scanning in large monolithic JARs parses thousands of .class files
        // using ASM ClassReader during startup, consuming CPU cycles and inflating container launch time.
        // With CandidateComponentsIndex active:
        // ClassPathScanningCandidateComponentProvider bypasses filesystem walking and loads
        // the pre-indexed bean candidate list in a single direct file read!
        HighThroughputBillingService service = new HighThroughputBillingService();
        String result = service.executeBilling(); // "billed"
    }
}
