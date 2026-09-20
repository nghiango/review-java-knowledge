package lab.concurrency.questions;

/** Q07: Demonstrates thread interruption and cooperative cancellation handling. */
@SuppressWarnings("unused")
public class Q07InterruptionHandlingExample {

    public static void main(String[] args) {
        Thread worker =
                new Thread(
                        () -> {
                            while (!Thread.currentThread().isInterrupted()) {
                                // Cooperative polling
                            }
                            boolean interruptedInWorker =
                                    Thread.currentThread().isInterrupted(); // true
                        });

        worker.start();
        worker.interrupt();

        boolean interruptedState = worker.isInterrupted(); // true
    }
}
