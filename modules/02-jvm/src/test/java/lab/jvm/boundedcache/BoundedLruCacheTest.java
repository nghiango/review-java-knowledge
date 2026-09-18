package lab.jvm.boundedcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class BoundedLruCacheTest {
    @Test
    void constructor_whenMaximumSizeIsNotPositive_rejectsCapacity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BoundedLruCache<String, String>(0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BoundedLruCache<String, String>(-1));
    }

    @Test
    void put_whenEntriesExceedCapacity_evictsLeastRecentlyUsedEntry() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);

        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");

        assertThat(cache.get("a")).isEmpty();
        assertThat(cache.snapshot()).containsExactly(Map.entry("b", "B"), Map.entry("c", "C"));
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void get_whenEntryRead_refreshesAccessOrder() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        cache.put("a", "A");
        cache.put("b", "B");

        assertThat(cache.get("a")).contains("A");
        cache.put("c", "C");

        assertThat(cache.get("b")).isEmpty();
        assertThat(cache.snapshot()).containsExactly(Map.entry("a", "A"), Map.entry("c", "C"));
    }

    @Test
    void put_whenExistingKeyReplaced_doesNotGrowCache() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);

        cache.put("a", "A");
        cache.put("a", "new-A");

        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.get("a")).contains("new-A");
    }

    @Test
    void computeIfAbsent_whenKeyAlreadyPresent_doesNotCallLoader() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        cache.put("a", "A");

        String value =
                cache.computeIfAbsent(
                        "a",
                        key -> {
                            throw new AssertionError("loader should not run");
                        });

        assertThat(value).isEqualTo("A");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void computeIfAbsent_whenMissing_addsComputedValueAndEvictsIfNeeded() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        cache.put("a", "A");
        cache.put("b", "B");

        String value = cache.computeIfAbsent("c", key -> key.toUpperCase(Locale.ROOT));

        assertThat(value).isEqualTo("C");
        assertThat(cache.get("a")).isEmpty();
        assertThat(cache.snapshot()).containsExactly(Map.entry("b", "B"), Map.entry("c", "C"));
    }

    @Test
    void snapshot_whenReturned_cannotMutateCache() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        cache.put("a", "A");

        Map<String, String> snapshot = cache.snapshot();

        assertThatThrownBy(() -> snapshot.put("b", "B"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(cache.snapshot()).containsExactly(Map.entry("a", "A"));
    }

    @Test
    void put_whenCalledConcurrently_leavesSizeBounded() throws InterruptedException {
        BoundedLruCache<Integer, Integer> cache = new BoundedLruCache<>(10);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Optional<Integer>>> tasks = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                int value = index;
                tasks.add(
                        () -> {
                            cache.put(value, value);
                            return cache.get(value);
                        });
            }

            executor.invokeAll(tasks);

            assertThat(cache.size()).isLessThanOrEqualTo(10);
            assertThat(cache.snapshot()).hasSizeLessThanOrEqualTo(10);
        } finally {
            executor.shutdownNow();
        }
    }
}
