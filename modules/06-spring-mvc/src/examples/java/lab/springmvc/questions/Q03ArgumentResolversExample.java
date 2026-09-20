package lab.springmvc.questions;

import java.util.Map;

public class Q03ArgumentResolversExample {

    record CurrentUser(String userId, String role) {}

    static class UserContextResolver {
        public CurrentUser resolveArgument(Map<String, String> headers) {
            String userId = headers.getOrDefault("X-User-Id", "anonymous");
            String role = headers.getOrDefault("X-User-Role", "USER");
            return new CurrentUser(userId, role);
        }
    }

    public static void main(String[] args) {
        UserContextResolver resolver = new UserContextResolver();
        CurrentUser user =
                resolver.resolveArgument(Map.of("X-User-Id", "usr-8841", "X-User-Role", "ADMIN"));

        String userId = user.userId(); // "usr-8841"
        String role = user.role(); // "ADMIN"
        boolean isAdmin = "ADMIN".equals(role); // true

        System.out.println(
                "Resolved user: " + userId + ", role: " + role + ", isAdmin: " + isAdmin);
    }
}
