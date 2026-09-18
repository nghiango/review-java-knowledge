package lab.jvm.questions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public final class Q23StaticListenerRetentionScenarioExample {
    private Q23StaticListenerRetentionScenarioExample() {}

    public interface Listener {
        void onEvent(String msg);
    }

    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    public static class EventRegistry {
        private final List<Listener> listeners = new ArrayList<>();

        public synchronized Registration register(Listener listener) {
            listeners.add(listener);
            var closed = new AtomicBoolean(false);
            return () -> {
                if (closed.compareAndSet(false, true)) {
                    synchronized (EventRegistry.this) {
                        listeners.remove(
                                listener); // deterministic unregistration prevents memory leak
                    }
                }
            };
        }

        public synchronized int size() {
            return listeners.size();
        }
    }

    public static void main(String[] args) throws Exception {
        var registry = new EventRegistry();
        Listener listener = event -> {};
        try (Registration reg = registry.register(listener)) {
            int active = registry.size(); // 1
        }
        int remaining = registry.size(); // 0 (unregistered upon scope exit)
    }
}
