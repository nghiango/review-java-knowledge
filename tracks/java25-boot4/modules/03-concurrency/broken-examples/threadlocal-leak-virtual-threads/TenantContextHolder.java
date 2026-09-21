package lab.java25boot4.concurrency.broken.threadlocalleak;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContextHolder() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void processTenantRequest(String tenantId, Runnable task) {
        setTenantId(tenantId);
        task.run();
    }
}
