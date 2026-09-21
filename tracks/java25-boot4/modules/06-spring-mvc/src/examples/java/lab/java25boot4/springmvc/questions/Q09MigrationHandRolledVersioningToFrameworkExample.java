package lab.java25boot4.springmvc.questions;

public class Q09MigrationHandRolledVersioningToFrameworkExample {

    // Migration from hand-rolled interceptor to Spring Framework 7 declarative version mapping
    public static void main(String[] args) {
        // Old: HandlerInterceptor inspects header -> request.setAttribute("version", h) ->
        // controller if/else
        // New: @GetMapping(value = "/path", headers = "X-API-Version=2.0") or
        // @RequestMapping(version = "2.0")
        boolean nativeFrameworkMapping = true;
        System.out.println(
                "Eliminates custom interceptor: "
                        + nativeFrameworkMapping); // Eliminates custom interceptor: true
    }
}
