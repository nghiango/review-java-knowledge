package lab.java25boot4.whatsnew.questions;

import java.util.Set;
import lab.java25boot4.whatsnew.securitygate.AccessDeniedException;
import lab.java25boot4.whatsnew.securitygate.Caller;
import lab.java25boot4.whatsnew.securitygate.ResourceAuthorizationGuard;

/** Q21: what replaces {@code SecurityManager}-based authorization on Java 25? */
public class Q21SecurityManagerReplacementExample {

    public static void main(String[] args) {
        ResourceAuthorizationGuard guard = new ResourceAuthorizationGuard();
        Caller admin = new Caller("alice", Set.of("admin.operations"));
        Caller reader = new Caller("bob", Set.of("read"));

        System.out.println(guard.isAuthorized(admin, "admin.operations")); // true
        System.out.println(guard.isAuthorized(reader, "admin.operations")); // false
        System.out.println(guard.isAuthorized(null, "admin.operations")); // false — fail closed

        try {
            guard.checkAuthorized(reader, "admin.operations");
        } catch (AccessDeniedException denied) {
            System.out.println(denied.getMessage()); // caller bob lacks authority admin.operations
        }
    }
}
