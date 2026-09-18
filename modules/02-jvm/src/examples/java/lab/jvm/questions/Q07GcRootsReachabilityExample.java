package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q07GcRootsReachabilityExample {
    private Q07GcRootsReachabilityExample() {}

    private static String staticRoot = "active-root"; // Static field is a permanent GC Root

    public static void main(String[] args) {
        String localStackRoot = "local-root"; // Local variable in active stack frame is a GC Root
        String transientObject = new String("short-lived"); // Reachable now

        transientObject = null; // Reference cleared; object becomes unreachable and eligible for GC
        String rootVal = staticRoot; // "active-root" (retained by classloader)
    }
}
