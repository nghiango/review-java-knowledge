package lab.java25boot4.springsecurity.broken.contextleak;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncAuditSecurityService {

    private final ExecutorService workerPool = Executors.newFixedThreadPool(2);

    public void dispatchPrivilegedAudit(String auditAction) {
        workerPool.submit(() -> {
            String currentUser = TenantSecurityContextHolder.getUsername();
            String currentRole = TenantSecurityContextHolder.getRole();

            if ("ROLE_ADMIN".equals(currentRole)) {
                executePrivilegedAuditMutation(auditAction, currentUser);
            }
        });
    }

    private void executePrivilegedAuditMutation(String action, String executedBy) {
        System.out.println("AUDIT_MUTATION: action=" + action + ", by=" + executedBy);
    }
}
