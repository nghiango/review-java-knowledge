package lab.jvm.questions;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class Q22UnboundedCacheGrowthScenarioExample {
    private Q22UnboundedCacheGrowthScenarioExample() {}

    public static class BoundedCache<K, V> extends LinkedHashMap<K, V> {
        private final int max;

        public BoundedCache(int max) {
            super(16, 0.75f, true);
            this.max = max;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > max; // LRU eviction protects heap from unbounded growth
        }
    }

    public static void main(String[] args) {
        BoundedCache<String, String> cache = new BoundedCache<>(2);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3"); // triggers LRU eviction of eldest entry "k1"

        int size = cache.size(); // 2 (bounded)
        boolean hasK1 = cache.containsKey("k1"); // false (safely evicted)
    }
}
