package lab.springcore.questions;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Q17: Demonstrates Spring AOP Pointcut expressions, @Around advice, and @Order precedence. */
@SuppressWarnings("unused")
public class Q17AopPointcutAdviceOrderingExample {

    @Aspect
    @Component
    @Order(1) // Highest precedence: executes first in chain
    static class SecurityAspect {
        @Around("execution(* lab.springcore..*.*(..))")
        public Object enforceSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
            return joinPoint.proceed();
        }
    }

    @Aspect
    @Component
    @Order(2) // Runs inside SecurityAspect
    static class MetricsAspect {
        @Around("execution(* lab.springcore..*.*(..))")
        public Object recordMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
            return joinPoint.proceed();
        }
    }

    public static void main(String[] args) {
        boolean orderedAspectsConfigured = true; // true
    }
}
