package lab.jvm.listenerregistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ListenerRegistry<T> {
    private final List<Entry<T>> listeners = new ArrayList<>();

    public Registration register(T listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        Entry<T> entry = new Entry<>(listener);
        synchronized (listeners) {
            listeners.add(entry);
        }
        return () -> {
            if (entry.close()) {
                synchronized (listeners) {
                    listeners.remove(entry);
                }
            }
        };
    }

    public List<T> snapshot() {
        synchronized (listeners) {
            List<T> snapshot = new ArrayList<>(listeners.size());
            for (Entry<T> entry : listeners) {
                snapshot.add(entry.listener());
            }
            return List.copyOf(snapshot);
        }
    }

    public int size() {
        synchronized (listeners) {
            return listeners.size();
        }
    }

    private record Entry<T>(T listener, AtomicBoolean closed) {
        private Entry(T listener) {
            this(listener, new AtomicBoolean());
        }

        private boolean close() {
            return closed.compareAndSet(false, true);
        }
    }
}
