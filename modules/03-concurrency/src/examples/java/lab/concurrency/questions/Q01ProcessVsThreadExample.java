package lab.concurrency.questions;

/** Q01: Demonstrates OS process isolation vs JVM thread memory sharing. */
@SuppressWarnings("unused")
public class Q01ProcessVsThreadExample {

    public static void main(String[] args) {
        long processId = ProcessHandle.current().pid(); // 12345 (OS Process ID)
        String threadName = Thread.currentThread().getName(); // "main"
        boolean isVirtual = Thread.currentThread().isVirtual(); // false

        // Threads in the same process share heap memory, while processes have isolated virtual
        // address spaces
        int availableCores =
                Runtime.getRuntime().availableProcessors(); // 8 (hardware execution contexts)
    }
}
