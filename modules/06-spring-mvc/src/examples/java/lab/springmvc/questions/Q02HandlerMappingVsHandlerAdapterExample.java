package lab.springmvc.questions;

public class Q02HandlerMappingVsHandlerAdapterExample {

    record MappingResult(String path, String controllerBean, String methodName) {}

    public static void main(String[] args) {
        // HandlerMapping identifies WHICH method should handle the request
        MappingResult mapping = new MappingResult("/api/users", "userController", "createUser");

        // HandlerAdapter knows HOW to execute the target method (parameter binding, argument
        // resolvers, return value handlers)
        boolean hasMappedMethod = mapping.methodName().equals("createUser"); // true
        boolean isUsersPath = mapping.path().equals("/api/users"); // true

        System.out.println(
                "Mapped: "
                        + mapping.controllerBean()
                        + "#"
                        + mapping.methodName()
                        + ", valid: "
                        + (hasMappedMethod && isUsersPath));
    }
}
