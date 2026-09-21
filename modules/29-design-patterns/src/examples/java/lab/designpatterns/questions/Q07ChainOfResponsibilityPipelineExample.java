package lab.designpatterns.questions;

/**
 * Q07: Chain of Responsibility Pattern. Demonstrates decoupling sender from receiver by passing
 * request through a handler chain.
 */
public class Q07ChainOfResponsibilityPipelineExample {

    public abstract static class AuthFilter {
        private AuthFilter next;

        public AuthFilter linkWith(AuthFilter next) {
            this.next = next;
            return next;
        }

        public boolean check(String user, String role) {
            if (!doCheck(user, role)) {
                return false; // Short-circuit
            }
            return next == null || next.check(user, role);
        }

        protected abstract boolean doCheck(String user, String role);
    }

    public static class UserExistsFilter extends AuthFilter {
        @Override
        protected boolean doCheck(String user, String role) {
            return user != null && !user.isBlank();
        }
    }

    public static class RoleAdminFilter extends AuthFilter {
        @Override
        protected boolean doCheck(String user, String role) {
            return "ADMIN".equals(role);
        }
    }

    public static void main(String[] args) {
        AuthFilter chain = new UserExistsFilter();
        chain.linkWith(new RoleAdminFilter());

        boolean guestAllowed =
                chain.check("guest_user", "GUEST"); // false (rejected by RoleAdminFilter)
        boolean adminAllowed = chain.check("root_user", "ADMIN"); // true (passed both filters)

        System.out.println("Q07 guest: " + guestAllowed + ", admin: " + adminAllowed);
    }
}
