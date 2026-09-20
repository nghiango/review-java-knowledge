package lab.springcore.selfinvocation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

class SelfInvocationProxyAspectTest {

    @Configuration
    @EnableAspectJAutoProxy
    static class AspectTestConfig {}

    @Test
    @DisplayName(
            "delegating to collaborator bean passes through Spring AOP proxy and triggers aspect")
    void collaboratorDelegation_triggersAspect() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(
                    AspectTestConfig.class,
                    AuditAspect.class,
                    OrderAuditService.class,
                    OrderProcessingService.class);
            context.refresh();

            OrderProcessingService processingService =
                    context.getBean(OrderProcessingService.class);
            AuditAspect aspect = context.getBean(AuditAspect.class);

            processingService.processOrder("ORD-2002");

            assertThat(aspect.getAuditLogs())
                    .hasSize(1)
                    .anyMatch(log -> log.contains("PROCESS_ORDER") && log.contains("ORD-2002"));
        }
    }
}
