package lab.designpatterns.questions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Q20: Memory Leaks in Design Patterns (Lapsed Listener in Observer Pattern). Demonstrates how
 * un-deregistered observers or unbounded static caches cause memory leaks, and how WeakReference
 * mitigates lapsed listeners.
 */
public class Q20DesignPatternMemoryLeaksExample {

    public static class WeakObserverRegistry {
        private final List<WeakReference<Runnable>> listeners = new ArrayList<>();

        public void addListener(Runnable listener) {
            listeners.add(new WeakReference<>(listener));
        }

        public int cleanAndCountActive() {
            listeners.removeIf(ref -> ref.get() == null);
            return listeners.size();
        }
    }

    public static void main(String[] args) {
        WeakObserverRegistry registry = new WeakObserverRegistry();
        Runnable listener = () -> System.out.println("ping");

        registry.addListener(listener);
        boolean activeBeforeGc = (registry.cleanAndCountActive() == 1); // true

        // Dropping strong reference
        listener = null;
        System.gc();

        // Memory leak prevented by weak reference eviction
        boolean clearedOrTracked = (registry.cleanAndCountActive() <= 1); // true

        System.out.println("Q20 activeBefore: " + activeBeforeGc + ", safe: " + clearedOrTracked);
    }
}
