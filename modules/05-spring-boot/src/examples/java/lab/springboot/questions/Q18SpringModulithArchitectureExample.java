package lab.springboot.questions;

import java.util.List;

public class Q18SpringModulithArchitectureExample {

    record ApplicationModule(
            String name, List<String> exposedPackages, List<String> internalPackages) {}

    public static void main(String[] args) {
        // Spring Modulith verifies logical boundaries between domain packages inside a monolithic
        // Spring Boot app
        ApplicationModule orderModule =
                new ApplicationModule(
                        "order",
                        List.of("lab.order.api", "lab.order.events"),
                        List.of("lab.order.internal"));

        boolean isApiExposed = orderModule.exposedPackages().contains("lab.order.api"); // true
        boolean isInternalHidden =
                !orderModule.exposedPackages().contains("lab.order.internal"); // true

        System.out.println(
                "Module: "
                        + orderModule.name()
                        + ", API exposed: "
                        + isApiExposed
                        + ", internal encapsulated: "
                        + isInternalHidden);
    }
}
