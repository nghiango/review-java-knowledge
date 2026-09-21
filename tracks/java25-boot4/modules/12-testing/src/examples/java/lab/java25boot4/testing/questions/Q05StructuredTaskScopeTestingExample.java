package lab.java25boot4.testing.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

/** Q05: How should StructuredTaskScope concurrency workflows be tested deterministically? */
public class Q05StructuredTaskScopeTestingExample {

    public static void main(String[] args) throws Exception {
        try (var scope =
                StructuredTaskScope.open(
                        StructuredTaskScope.Joiner.<String>awaitAllSuccessfulOrThrow())) {
            Subtask<String> taskA = scope.fork(() -> "ResultA");
            Subtask<String> taskB = scope.fork(() -> "ResultB");

            scope.join();

            boolean stateA = taskA.state() == Subtask.State.SUCCESS;
            boolean stateB = taskB.state() == Subtask.State.SUCCESS;

            System.out.println("Subtask A succeeded: " + stateA); // Subtask A succeeded: true
            System.out.println("Subtask B succeeded: " + stateB); // Subtask B succeeded: true
            System.out.println(
                    "Values: " + taskA.get() + "," + taskB.get()); // Values: ResultA,ResultB
        }
    }
}
