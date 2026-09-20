package lab.springcore.questions;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Q13: Demonstrates resolving Prototype-in-Singleton injection using ObjectProvider. */
@SuppressWarnings({"unused", "ReferenceEquality"})
public class Q13PrototypeInSingletonLookupExample {

    @Component
    @Scope("prototype")
    static class TaskToken {}

    @Component
    static class TokenConsumer {
        private final ObjectProvider<TaskToken> tokenProvider;

        public TokenConsumer(ObjectProvider<TaskToken> tokenProvider) {
            this.tokenProvider = tokenProvider;
        }

        public TaskToken acquireNewToken() {
            return tokenProvider.getObject();
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(TaskToken.class, TokenConsumer.class);
            context.refresh();

            TokenConsumer consumer = context.getBean(TokenConsumer.class);
            TaskToken t1 = consumer.acquireNewToken();
            TaskToken t2 = consumer.acquireNewToken();

            boolean distinctTokens = (t1 != t2); // true (fresh prototype instance per call)
        }
    }
}
