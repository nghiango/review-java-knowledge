package lab.jpahibernate.questions;

public class Q07FirstVsSecondLevelCache {

    public static void main(String[] args) {
        // L1 Cache: Scoped to PersistenceContext / Session, always enabled, non-sharable across
        // threads
        String l1Scope = "Session / EntityManager";
        boolean l1EnabledByDefault = true; // true
        boolean l1ThreadSafe = false; // false (Session is not thread-safe)

        // L2 Cache: Scoped to SessionFactory / JVM cluster, optional (Ehcache, Infinispan,
        // Hazelcast), shared across transactions
        String l2Scope = "SessionFactory / Application";
        boolean l2EnabledByDefault = false; // false
        boolean l2StoresHydratedState =
                true; // true (stores disassembled state tuples, not managed entity objects)

        System.out.println("L1 scope: " + l1Scope); // L1 scope: Session / EntityManager
        System.out.println(
                "L1 enabled by default: " + l1EnabledByDefault); // L1 enabled by default: true
        System.out.println("L1 thread-safe: " + l1ThreadSafe); // L1 thread-safe: false
        System.out.println("L2 scope: " + l2Scope); // L2 scope: SessionFactory / Application
        System.out.println(
                "L2 enabled by default: " + l2EnabledByDefault); // L2 enabled by default: false
        System.out.println(
                "L2 stores disassembled state: "
                        + l2StoresHydratedState); // L2 stores disassembled state: true
    }
}
