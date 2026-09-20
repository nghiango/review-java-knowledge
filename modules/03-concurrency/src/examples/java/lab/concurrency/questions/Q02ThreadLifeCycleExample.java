package lab.concurrency.questions;

/** Q02: Demonstrates the 6 thread lifecycle states in Java. */
@SuppressWarnings("unused")
public class Q02ThreadLifeCycleExample {

    public static void main(String[] args) throws InterruptedException {
        Thread thread =
                new Thread(
                        () -> {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });

        Thread.State stateNew = thread.getState(); // Thread.State.NEW
        thread.start();

        Thread.State stateRunning = thread.getState(); // Thread.State.RUNNABLE (or TIMED_WAITING)
        thread.join();

        Thread.State stateTerminated = thread.getState(); // Thread.State.TERMINATED
    }
}
