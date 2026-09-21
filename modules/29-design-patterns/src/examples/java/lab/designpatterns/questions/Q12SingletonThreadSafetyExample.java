package lab.designpatterns.questions;

/**
 * Q12: Thread-Safe Singleton: Double-Checked Locking with volatile vs Initialization-on-demand
 * Holder. Demonstrates the Bill Pugh Holder idiom guaranteeing thread-safe lazy initialization.
 */
public class Q12SingletonThreadSafetyExample {

    // Initialization-on-demand holder idiom (JVM classloading guarantees thread safety without
    // synchronization overhead)
    public static class ThreadSafeSingleton {
        private ThreadSafeSingleton() {}

        private static class Holder {
            private static final ThreadSafeSingleton INSTANCE = new ThreadSafeSingleton();
        }

        public static ThreadSafeSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    @SuppressWarnings("ReferenceEquality")
    public static void main(String[] args) {
        ThreadSafeSingleton s1 = ThreadSafeSingleton.getInstance();
        ThreadSafeSingleton s2 = ThreadSafeSingleton.getInstance();

        boolean sameInstance = (s1 == s2); // true (singleton reference identity)

        System.out.println("Q12 sameInstance: " + sameInstance);
    }
}
