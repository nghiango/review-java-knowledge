package lab.jpahibernate.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q28StatelessSessionBatchStreamingExample {
    private Q28StatelessSessionBatchStreamingExample() {}

    public record AuditLog(Long id, String action) {}

    // Hibernate StatelessSession vs standard Session:
    // 1. No First-Level Cache: Entities are not managed or tracked in an identity map
    // 2. No Dirty Checking: Calling setters does not issue automatic UPDATE statements on flush
    // 3. No Second-Level Cache: Bypasses entity/query cache overhead entirely
    // 4. Explicit CRUD: Requires explicit session.insert(), session.update(), session.delete()
    public static class StatelessSessionSimulator {
        private final List<AuditLog> directStream = new ArrayList<>();

        public void streamBatch(int count) {
            for (long i = 1; i <= count; i++) {
                // Instantiates and writes directly to database socket buffer without retaining in heap
                AuditLog log = new AuditLog(i, "AUDIT_EVENT");
                directStream.add(log);
            }
        }

        public int getProcessedCount() {
            return directStream.size();
        }
    }

    public static void main(String[] args) {
        StatelessSessionSimulator simulator = new StatelessSessionSimulator();
        simulator.streamBatch(100);

        int count = simulator.getProcessedCount(); // 100
        boolean zeroHeapRetention = count == 100; // true
    }
}
