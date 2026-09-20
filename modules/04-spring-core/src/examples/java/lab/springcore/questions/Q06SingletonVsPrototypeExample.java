package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Q06: Demonstrates Singleton vs Prototype bean scopes. */
@SuppressWarnings({"unused", "ReferenceEquality"})
public class Q06SingletonVsPrototypeExample {

    @Component
    @Scope("singleton")
    static class SingletonBean {}

    @Component
    @Scope("prototype")
    static class PrototypeBean {}

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(SingletonBean.class, PrototypeBean.class);
            context.refresh();

            SingletonBean s1 = context.getBean(SingletonBean.class);
            SingletonBean s2 = context.getBean(SingletonBean.class);
            boolean sameSingleton = (s1 == s2); // true (single shared instance)

            PrototypeBean p1 = context.getBean(PrototypeBean.class);
            PrototypeBean p2 = context.getBean(PrototypeBean.class);
            boolean samePrototype = (p1 == p2); // false (new instance per getBean)
        }
    }
}
