package lab.java25boot4.springsecurity.broken.contextleak;

public final class TenantSecurityContextHolder {

    private static final InheritableThreadLocal<String> CURRENT_USER = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> CURRENT_ROLE = new InheritableThreadLocal<>();

    private TenantSecurityContextHolder() {}

    public static void setContext(String username, String role) {
        CURRENT_USER.set(username);
        CURRENT_ROLE.set(role);
    }

    public static String getUsername() {
        return CURRENT_USER.get();
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLE.remove();
    }
}
