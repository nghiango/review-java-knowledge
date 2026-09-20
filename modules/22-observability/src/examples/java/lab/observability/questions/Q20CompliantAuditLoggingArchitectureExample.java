package lab.observability.questions;

public class Q20CompliantAuditLoggingArchitectureExample {

    record AuditLogEntry(
            String eventId,
            String actor,
            String action,
            String resource,
            boolean encryptedAtRest) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Regulatory compliance (SOC2, PCI-DSS, GDPR, HIPAA):
        // 1. Immutability: Audit logs append-only (WORM - Write Once Read Many storage like AWS S3
        // Object Lock).
        // 2. Encryption: Encrypted at rest using KMS envelope encryption.
        // 3. Asynchronous offloading: Dispatched via dedicated bounded queues (Disruptor / Kafka)
        // so audit I/O never blocks business transactions.
        AuditLogEntry entry =
                new AuditLogEntry("evt-101", "admin@internal", "TRANSFER_FUNDS", "acc-9988", true);

        boolean isEncrypted = entry.encryptedAtRest(); // true
        boolean hasActorAndAction = entry.actor() != null && entry.action() != null; // true

        System.out.println("Audit log entry encrypted: " + isEncrypted);
        System.out.println("Audit entry contains actor attribution: " + hasActorAndAction);
    }
}
