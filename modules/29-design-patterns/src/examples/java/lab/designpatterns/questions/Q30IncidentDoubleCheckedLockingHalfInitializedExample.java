package lab.designpatterns.questions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstrates a production incident where a broken Double-Checked Locking Singleton implementation
 * lacked the {@code volatile} keyword, allowing reordered memory instructions to expose half-initialized objects.
 *
 * <p>Fix: Declare the instance field {@code volatile} to enforce Java Memory Model happens-before edge.</p>
 */
public class Q30IncidentDoubleCheckedLockingHalfInitializedExample {

    public static class ThreadSafeSingleton {
        private static volatile ThreadSafeSingleton instance;
        private final boolean initialized;

        private ThreadSafeSingleton() {
            // Expensive initialization simulated
            this.initialized = true;
        }

        public static ThreadSafeSingleton getInstance() {
            if (instance == null) {
                synchronized (ThreadSafeSingleton.class) {
                    if (instance == null) {
                        instance = new ThreadSafeSingleton();
                    }
                }
            }
            return instance;
        }

        public boolean isInitialized() {
            return initialized;
        }
    }

    public static void main(String[] args) {
        ThreadSafeSingleton singleton = ThreadSafeSingleton.getInstance();
        boolean fullyConstructed = singleton != null && singleton.isInitialized(); // true

        System.out.println("Singleton fully constructed before publication: " + fullyConstructed);
    }
}
