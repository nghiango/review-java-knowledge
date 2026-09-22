package lab.java25boot4.whatsnew.broken.securitygate;

import java.security.AccessController;
import java.security.Permission;

/**
 * Guards destructive administrative operations. The intent is that only callers the JVM has granted
 * the admin permission may proceed, so the check is centralised here.
 */
public class SecurityManagerGate {

    private static final Permission ADMIN_PERMISSION = new RuntimePermission("admin.operations");

    public boolean isAuthorized(String userId) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager == null) {
            return true;
        }
        try {
            AccessController.checkPermission(ADMIN_PERMISSION);
            return true;
        } catch (SecurityException denied) {
            return false;
        }
    }
}
