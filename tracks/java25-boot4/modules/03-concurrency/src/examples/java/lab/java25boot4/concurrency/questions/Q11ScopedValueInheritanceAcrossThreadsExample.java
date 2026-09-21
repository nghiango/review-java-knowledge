package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;

public class Q11ScopedValueInheritanceAcrossThreadsExample {

    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        ScopedValue.where(TRACE_ID, "trace-999")
                .run(
                        () -> {
                            try (var scope = StructuredTaskScope.open()) {
                                // Forked virtual subtasks automatically inherit scoped values from
                                // parent scope
                                var subtask =
                                        scope.fork(
                                                () -> "Subtask observed trace: " + TRACE_ID.get());

                                scope.join();
                                System.out.println(
                                        subtask.get()); // Subtask observed trace: trace-999
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
    }
}
