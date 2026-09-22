package lab.java25boot4.whatsnew.securitygate;

/**
 * Replaces {@code SecurityManager}-based authorization, which is permanently disabled on Java 25.
 *
 * <p>Decision: the check is a pure function of the caller and the required authority and it
 * <b>fails closed</b> — an unknown or absent caller is denied. Nothing about the decision depends on
 * a JVM-global flag, so it cannot be silently disabled by a missing startup option.
 *
 * <p>Trade-off: the application now owns the authority model. In a Spring service you would delegate
 * to Spring Security's {@code AuthorizationManager}; the fail-closed principle is the same.
 */
public final class ResourceAuthorizationGuard {

    public boolean isAuthorized(Caller caller, String requiredAuthority) {
        if (caller == null || requiredAuthority == null || requiredAuthority.isBlank()) {
            return false;
        }
        return caller.authorities().contains(requiredAuthority);
    }

    public void checkAuthorized(Caller caller, String requiredAuthority) {
        if (!isAuthorized(caller, requiredAuthority)) {
            throw new AccessDeniedException(
                    "caller "
                            + (caller == null ? "<anonymous>" : caller.id())
                            + " lacks authority "
                            + requiredAuthority);
        }
    }
}
