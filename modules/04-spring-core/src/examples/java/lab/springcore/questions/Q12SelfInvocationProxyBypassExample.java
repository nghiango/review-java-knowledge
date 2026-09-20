package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/** Q12: Demonstrates the Spring AOP self-invocation proxy bypass issue. */
@SuppressWarnings("unused")
public class Q12SelfInvocationProxyBypassExample {

    @Component
    static class Worker {
        public String externalEntry() {
            // Calling annotatedMethod directly on 'this' bypasses the Spring CGLIB/JDK proxy!
            return internalWork();
        }

        public String internalWork() {
            return "DONE";
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {}

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(Config.class, Worker.class);
            context.refresh();

            Worker worker = context.getBean(Worker.class);
            String result =
                    worker.externalEntry(); // "DONE" (self-invocation does not pass through proxy)
        }
    }
}
