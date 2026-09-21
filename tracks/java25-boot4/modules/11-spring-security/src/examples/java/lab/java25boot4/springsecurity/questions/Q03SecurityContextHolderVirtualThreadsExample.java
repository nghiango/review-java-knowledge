package lab.java25boot4.springsecurity.questions;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Q03: How does SecurityContextHolder interact with virtual threads in Spring Boot 4, and what is
 * the default strategy?
 */
public class Q03SecurityContextHolderVirtualThreadsExample {

    public static void main(String[] args) {
        SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();

        // Default strategy is ThreadLocal-backed, meaning virtual threads have their own isolated
        // context
        String strategyName = strategy.getClass().getSimpleName();
        boolean isVirtualThread = Thread.currentThread().isVirtual();

        System.out.println(
                "Holder Strategy: " + strategyName); // "ThreadLocalSecurityContextHolderStrategy"
        System.out.println("Current thread virtual: " + isVirtualThread); // false
        System.out.println("Context non-null: " + (strategy.getContext() != null)); // true
    }
}
