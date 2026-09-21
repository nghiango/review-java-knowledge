package lab.java25boot4.concurrency.questions;

public class Q03VirtualThreadPinningFixExample {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws Exception {
        Thread vt =
                Thread.ofVirtual()
                        .name("vt-unpinned-demo")
                        .start(
                                () -> {
                                    synchronized (LOCK) {
                                        try {
                                            // In Java 21, sleep/blocking inside synchronized pinned
                                            // carrier thread.
                                            // In Java 25, ObjectMonitors support virtual thread
                                            // unmounting cleanly.
                                            Thread.sleep(50);
                                            System.out.println(
                                                    "Finished inside synchronized: "
                                                            + Thread.currentThread()
                                                                    .isVirtual()); // Finished
                                            // inside
                                            // synchronized:
                                            // true
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                                });

        vt.join();
    }
}
