package lab.corejava.questions;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class Q17MutableKeyFailureExample {
    private Q17MutableKeyFailureExample() {}

    public static class MutableKey {
        public String id;

        public MutableKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MutableKey other && this.id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static void main(String[] args) {
        Map<MutableKey, String> cache = new HashMap<>();
        MutableKey key = new MutableKey("tenant-A");
        cache.put(key, "data-A");

        key.id = "tenant-B"; // MUTATION after insertion!

        String found = cache.get(key); // null (lookup searches in new hash bucket, misses entry!)
        int size = cache.size(); // 1 (entry is still orphaned in memory)
    }
}
