package lab.java25boot4.whatsnew.broken.securitygate;

/**
 * Administrative operations that must only run for authorized callers.
 */
public class AdminOperationService {

    private final SecurityManagerGate gate;

    public AdminOperationService(SecurityManagerGate gate) {
        this.gate = gate;
    }

    public String purgeTenantData(String userId) {
        if (!gate.isAuthorized(userId)) {
            return "denied";
        }
        return "purged tenant data for " + userId;
    }
}
