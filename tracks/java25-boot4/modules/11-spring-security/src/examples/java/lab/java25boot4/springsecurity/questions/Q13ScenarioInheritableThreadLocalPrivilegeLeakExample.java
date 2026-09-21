package lab.java25boot4.springsecurity.questions;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Q13: Scenario: An async worker thread executed an audit mutation with admin rights left over from
 * a previous task. How does ScopedValue prevent identity spoofing?
 */
public class Q13ScenarioInheritableThreadLocalPrivilegeLeakExample {

    public static final ScopedValue<String> IDENTITY = ScopedValue.newInstance();

    public static void main(String[] args) {
        AtomicReference<String> task1Identity = new AtomicReference<>();
        AtomicReference<String> task2Identity = new AtomicReference<>();

        // Task 1 runs with admin identity
        ScopedValue.where(IDENTITY, "ADMIN_OPERATOR")
                .run(
                        () -> {
                            task1Identity.set(IDENTITY.get());
                        });

        // Task 2 runs in an unauthenticated / guest scope on the same thread
        ScopedValue.where(IDENTITY, "GUEST_USER")
                .run(
                        () -> {
                            task2Identity.set(IDENTITY.get());
                        });

        System.out.println("Task 1 identity: " + task1Identity.get()); // "ADMIN_OPERATOR"
        System.out.println("Task 2 identity: " + task2Identity.get()); // "GUEST_USER"
        System.out.println(
                "Task 2 never saw admin: " + !"ADMIN_OPERATOR".equals(task2Identity.get())); // true
    }
}
