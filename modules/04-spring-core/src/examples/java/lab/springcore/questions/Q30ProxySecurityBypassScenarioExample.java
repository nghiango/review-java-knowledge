package lab.springcore.questions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.aop.framework.AopContext;

@SuppressWarnings("unused")
public final class Q30ProxySecurityBypassScenarioExample {
    private Q30ProxySecurityBypassScenarioExample() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SecuredRole {
        String value();
    }

    public static class SensitiveOrderService {

        // Public entrypoint without explicit security annotation
        public void processOrder(Long orderId) {
            // Self-invocation bug: calls executeDirectRefund() directly via 'this'!
            // 'this' refers to the raw unproxied target instance, completely bypassing Spring AOP proxy interceptor!
            executeDirectRefund(orderId);
        }

        // Sensitive method annotated with security check
        @SecuredRole("ADMIN")
        public void executeDirectRefund(Long orderId) {
            // Should require ADMIN role checked by AOP advice!
        }

        // Remediated pattern: routes call through the current AOP proxy
        public void processOrderSecured(Long orderId) {
            // Requires @EnableAspectJAutoProxy(exposeProxy = true)
            ((SensitiveOrderService) AopContext.currentProxy()).executeDirectRefund(orderId);
        }
    }

    public static void main(String[] args) {
        SensitiveOrderService service = new SensitiveOrderService();
        boolean initialized = service != null; // true
        // Self-invocation bypasses all Spring proxy interceptors (@Transactional, @Secured, @Cacheable)
    }
}
