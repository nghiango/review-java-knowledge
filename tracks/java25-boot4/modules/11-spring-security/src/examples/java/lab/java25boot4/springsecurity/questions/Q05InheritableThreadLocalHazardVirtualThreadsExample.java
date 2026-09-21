package lab.java25boot4.springsecurity.questions;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Q05: Why is SecurityContextHolder.MODE_INHERITABLETHREADLOCAL hazardous when applications run on
 * virtual threads?
 */
public class Q05InheritableThreadLocalHazardVirtualThreadsExample {

    private static final InheritableThreadLocal<String> CONTEXT = new InheritableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        CONTEXT.set("ADMIN_TOKEN");

        AtomicReference<String> workerObserved = new AtomicReference<>();

        // Spawning thousands of virtual threads copying InheritableThreadLocal causes memory bloat
        // and carrier leakage
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    workerObserved.set(CONTEXT.get());
                                });

        vt.join();
        CONTEXT.remove();

        System.out.println(
                "Worker observed parent context: "
                        + "ADMIN_TOKEN".equals(workerObserved.get())); // true
        System.out.println("Parent context cleared: " + (CONTEXT.get() == null)); // true
    }
}
