package lab.springcore.questions;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/** Q07: Demonstrates Spring Bean Lifecycle callbacks (@PostConstruct, @PreDestroy). */
@SuppressWarnings("unused")
public class Q07BeanLifecyclePhasesExample {

    @Component
    static class LifecycleBean {
        private boolean initialized = false;
        private boolean destroyed = false;

        @PostConstruct
        public void init() {
            this.initialized = true;
        }

        @PreDestroy
        public void cleanup() {
            this.destroyed = true;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public boolean isDestroyed() {
            return destroyed;
        }
    }

    public static void main(String[] args) {
        LifecycleBean beanRef;
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(LifecycleBean.class);
            context.refresh();

            beanRef = context.getBean(LifecycleBean.class);
            boolean postConstructRan = beanRef.isInitialized(); // true
        }

        boolean preDestroyRan = beanRef.isDestroyed(); // true (on context close)
    }
}
