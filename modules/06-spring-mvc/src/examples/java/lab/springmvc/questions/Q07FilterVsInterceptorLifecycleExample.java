package lab.springmvc.questions;

import java.util.ArrayList;
import java.util.List;

public class Q07FilterVsInterceptorLifecycleExample {

    public static void main(String[] args) {
        // Lifecycle execution order for an HTTP request
        List<String> executionOrder = new ArrayList<>();

        executionOrder.add("1. Filter (doFilter - before)");
        executionOrder.add("2. DispatcherServlet");
        executionOrder.add("3. HandlerInterceptor (preHandle)");
        executionOrder.add("4. Controller HandlerMethod");
        executionOrder.add("5. HandlerInterceptor (postHandle)");
        executionOrder.add("6. HandlerInterceptor (afterCompletion)");
        executionOrder.add("7. Filter (doFilter - after)");

        String firstStep = executionOrder.get(0); // "1. Filter (doFilter - before)"
        String controllerStep = executionOrder.get(3); // "4. Controller HandlerMethod"
        int totalHooks = executionOrder.size(); // 7

        System.out.println(
                "First: "
                        + firstStep
                        + ", controller: "
                        + controllerStep
                        + ", total hooks: "
                        + totalHooks);
    }
}
