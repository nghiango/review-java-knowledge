package lab.java25boot4.springsecurity.questions;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Q06: How do Java 25 Scoped Values solve security context propagation across concurrent tasks with
 * zero memory leaks?
 */
public class Q06ScopedValueSecurityContextPropagationExample {

    public static final ScopedValue<String> PRINCIPAL = ScopedValue.newInstance();

    public static void main(String[] args) throws InterruptedException {
        AtomicReference<String> workerPrincipal = new AtomicReference<>();

        ScopedValue.where(PRINCIPAL, "service-account-checkout")
                .run(
                        () -> {
                            Thread vt =
                                    Thread.ofVirtual()
                                            .start(
                                                    () -> {
                                                        // ScopedValue is automatically inherited by
                                                        // child threads without copying maps or
                                                        // leaking
                                                        workerPrincipal.set(
                                                                PRINCIPAL.isBound()
                                                                        ? PRINCIPAL.get()
                                                                        : "ANONYMOUS");
                                                    });
                            try {
                                vt.join();
                            } catch (InterruptedException ignored) {
                            }
                        });

        System.out.println(
                "Child thread read principal: "
                        + workerPrincipal.get()); // "service-account-checkout"
        System.out.println("Principal bound outside: " + PRINCIPAL.isBound()); // false
    }
}
