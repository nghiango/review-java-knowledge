package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/** Q19: Demonstrates Scoped Proxies with ScopedProxyMode.TARGET_CLASS. */
@SuppressWarnings("unused")
public class Q19ScopedProxiesTargetClassExample {

    // Scoped proxy wraps the prototype in a CGLIB proxy; the proxy is injected once into
    // singletons,
    // but delegates each method call to a fresh prototype target instance.
    @Component
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    static class ScopedWorker {
        private final long createdTimestamp = System.nanoTime();

        public long getCreatedTimestamp() {
            return createdTimestamp;
        }
    }

    @Component
    static class SingletonMaster {
        private final ScopedWorker worker;

        public SingletonMaster(ScopedWorker worker) {
            this.worker = worker;
        }

        public long executeTask() {
            return worker.getCreatedTimestamp();
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(ScopedWorker.class, SingletonMaster.class);
            context.refresh();

            SingletonMaster master = context.getBean(SingletonMaster.class);
            long t1 = master.executeTask();
            long t2 = master.executeTask();

            boolean distinctRuns = (t1 != t2); // true (proxy resolves fresh target per invocation)
        }
    }
}
