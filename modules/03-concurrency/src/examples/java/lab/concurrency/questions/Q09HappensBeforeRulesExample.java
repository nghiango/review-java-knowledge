package lab.concurrency.questions;

/**
 * Q09: Demonstrates Java Memory Model happens-before rules (thread start/join, volatile, monitor
 * lock).
 */
@SuppressWarnings("unused")
public class Q09HappensBeforeRulesExample {

    private static int sharedValue = 0;

    public static void main(String[] args) throws InterruptedException {
        sharedValue = 100;

        // Rule 1: Thread.start() happens-before any action in the started thread
        Thread thread =
                new Thread(
                        () -> {
                            int readInsideThread =
                                    sharedValue; // 100 (guaranteed visible by thread start rule)
                            sharedValue = 200;
                        });

        thread.start();
        // Rule 2: All actions in thread happen-before a successful join() returns on that thread
        thread.join();

        int readAfterJoin = sharedValue; // 200 (guaranteed visible by thread join rule)
    }
}
