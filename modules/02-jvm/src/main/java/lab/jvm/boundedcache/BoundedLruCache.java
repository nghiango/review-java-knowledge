package lab.jvm.boundedcache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class BoundedLruCache<K, V> {
    private final Map<K, V> entries;

    public BoundedLruCache(int maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be greater than zero");
        }
        this.entries =
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                        return size() > maximumSize;
                    }
                };
    }

    public synchronized Optional<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(entries.get(key));
    }

    public synchronized void put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        entries.put(key, value);
    }

    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        V value = entries.get(key);
        if (value != null) {
            return value;
        }
        V loaded = Objects.requireNonNull(loader.apply(key), "loader returned null");
        entries.put(key, loaded);
        return loaded;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized Map<K, V> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }
}
