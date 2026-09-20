package lab.springmvc.questions;

import java.util.List;

public class Q01DispatcherServletFlowExample {

    record DispatcherPhase(int sequence, String component, String action) {}

    public static void main(String[] args) {
        // DispatcherServlet coordinates request dispatching across standard pipeline components
        List<DispatcherPhase> pipeline =
                List.of(
                        new DispatcherPhase(1, "DispatcherServlet", "Receives HttpServletRequest"),
                        new DispatcherPhase(
                                2,
                                "HandlerMapping",
                                "Resolves RequestMappingHandlerMapping to HandlerMethod"),
                        new DispatcherPhase(
                                3,
                                "HandlerAdapter",
                                "Invokes RequestMappingHandlerAdapter with ArgumentResolvers"),
                        new DispatcherPhase(
                                4,
                                "HttpMessageConverter",
                                "Reads RequestBody and writes ResponseBody (Jackson)"),
                        new DispatcherPhase(
                                5,
                                "HandlerInterceptor",
                                "postHandle and afterCompletion lifecycle hooks"));

        String firstComponent = pipeline.get(0).component(); // "DispatcherServlet"
        String adapterComponent = pipeline.get(2).component(); // "HandlerAdapter"
        int totalPhases = pipeline.size(); // 5

        System.out.println(
                "Pipeline root: "
                        + firstComponent
                        + ", adapter: "
                        + adapterComponent
                        + ", phases: "
                        + totalPhases);
    }
}
