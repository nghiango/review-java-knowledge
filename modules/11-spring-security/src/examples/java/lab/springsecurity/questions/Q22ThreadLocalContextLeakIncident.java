package lab.springsecurity.questions;

public class Q22ThreadLocalContextLeakIncident {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void main(String[] args) {
        // Request 1: Authenticated admin executes on worker thread
        CONTEXT_HOLDER.set("admin_principal");
        String req1User = CONTEXT_HOLDER.get(); // "admin_principal"

        // Incident: Request 1 completes without calling CONTEXT_HOLDER.remove() in finally block!
        boolean leakedOnThread = (CONTEXT_HOLDER.get() != null); // true

        // Request 2: Unauthenticated / anonymous visitor reuses the pooled thread
        String req2ObservedUser =
                CONTEXT_HOLDER.get(); // "admin_principal" (Critical Security Context Leakage!)

        // Remediation: Always clean in finally block
        CONTEXT_HOLDER.remove();
        boolean isSafelyCleaned = (CONTEXT_HOLDER.get() == null); // true

        System.out.println("Request 1 user: " + req1User); // Request 1 user: admin_principal
        System.out.println(
                "ThreadLocal retained on pooled thread: "
                        + leakedOnThread); // ThreadLocal retained on pooled thread: true
        System.out.println(
                "Request 2 erroneously inherited admin identity: "
                        + "admin_principal"
                                .equals(req2ObservedUser)); // Request 2 erroneously inherited admin
        // identity: true
        System.out.println(
                "Remediation safely cleared context: "
                        + isSafelyCleaned); // Remediation safely cleared context: true
    }
}
