package lab.java25boot4.springsecurity.questions;

import java.util.Set;

/**
 * Q12: How do RoleHierarchy and modern AuthorizationManager collaborate to provide transitive
 * role-based authorization?
 */
public class Q12RoleHierarchyAuthorizationManagerExample {

    public static class SimpleRoleHierarchy {
        public Set<String> getReachableRoles(String role) {
            if ("ROLE_SUPERADMIN".equals(role)) {
                return Set.of("ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_USER");
            } else if ("ROLE_ADMIN".equals(role)) {
                return Set.of("ROLE_ADMIN", "ROLE_USER");
            }
            return Set.of("ROLE_USER");
        }
    }

    public static void main(String[] args) {
        SimpleRoleHierarchy hierarchy = new SimpleRoleHierarchy();

        var adminReachable = hierarchy.getReachableRoles("ROLE_ADMIN");

        System.out.println("Admin has ROLE_USER: " + adminReachable.contains("ROLE_USER")); // true
        System.out.println(
                "Admin has ROLE_SUPERADMIN: "
                        + adminReachable.contains("ROLE_SUPERADMIN")); // false
        System.out.println("Reachable count: " + adminReachable.size()); // 2
    }
}
