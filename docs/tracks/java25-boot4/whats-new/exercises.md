# Exercises: Java 25 & Spring Boot 4 Katas

!!! info "Delta from baseline"
    Baseline modules provide practice exercises on core Java data structures and Spring Boot patterns.
    These hands-on exercises focus on migrating legacy code to **Java 25 LTS** APIs: replacing `sun.misc.Unsafe` with the Foreign Function & Memory API, and designing stateful stream pipelines with Stream Gatherers.

---

## Exercise 1: Migrating Off-Heap Buffer to Foreign Function & Memory API

### Problem Statement
You are modernizing a legacy low-latency messaging cache. The current implementation uses `sun.misc.Unsafe` to store integer sequences in off-heap memory. On Java 25, compiler warnings are triggered, and any out-of-bounds index corrupts native memory.

Refactor the class to:
1. Eliminate all references to `sun.misc.Unsafe`.
2. Manage off-heap memory via `java.lang.foreign.Arena` and `MemorySegment`.
3. Enforce spatial bounds checking.
4. Implement `AutoCloseable` to guarantee deterministic memory reclamation.

### Starting Point (Legacy Unsafe Code)
```java
class LegacyIntBuffer {
    private static final sun.misc.Unsafe UNSAFE = getUnsafe();
    private final long address;
    private final int capacity;

    public LegacyIntBuffer(int capacity) {
        this.capacity = capacity;
        this.address = UNSAFE.allocateMemory((long) capacity * Integer.BYTES);
    }

    public void put(int index, int value) {
        UNSAFE.putInt(address + (long) index * Integer.BYTES, value);
    }

    public int get(int index) {
        return UNSAFE.getInt(address + (long) index * Integer.BYTES);
    }

    public void release() {
        UNSAFE.freeMemory(address);
    }
}
```

??? question "Reveal solution"

    ### Solution
    ```java
    package lab.java25boot4.whatsnew.exercises;

    import java.lang.foreign.Arena;
    import java.lang.foreign.MemorySegment;
    import java.lang.foreign.ValueLayout;

    public final class ModernIntBuffer implements AutoCloseable {

        private final Arena arena;
        private final MemorySegment segment;
        private final int capacity;

        public ModernIntBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive: " + capacity);
            }
            this.capacity = capacity;
            this.arena = Arena.ofConfined();
            this.segment = arena.allocate((long) capacity * Integer.BYTES);
        }

        public void put(int index, int value) {
            checkBounds(index);
            segment.set(ValueLayout.JAVA_INT, (long) index * Integer.BYTES, value);
        }

        public int get(int index) {
            checkBounds(index);
            return segment.get(ValueLayout.JAVA_INT, (long) index * Integer.BYTES);
        }

        public int capacity() {
            return capacity;
        }

        @Override
        public void close() {
            arena.close();
        }

        private void checkBounds(int index) {
            if (index < 0 || index >= capacity) {
                throw new IndexOutOfBoundsException("index " + index + " out of bounds for capacity " + capacity);
            }
        }
    }
    ```

    **Key Points:**
    - Uses `Arena.ofConfined()` for single-thread deterministic lifecycle.
    - Uses `ValueLayout.JAVA_INT` for type-safe memory offsets.
    - Implements `AutoCloseable` so memory can be scoped in try-with-resources.
    - Intrinsic bounds checks ensure no memory corruption can occur outside the segment.

---

## Exercise 2: Implementing Moving Averages with Stream Gatherers

### Problem Statement
In financial analytics, a 3-period moving average must be calculated over a continuous stream of prices. In Java 21, doing this within a stream required external state holders or custom collectors.

Using Java 25's `Stream::gather` and `Gatherers.windowSliding`:
1. Take a `List<Double>` of price points.
2. Form sliding windows of size 3 (advancing by 1 element per window).
3. Compute the arithmetic mean of each window.
4. Collect the calculated moving averages into a resulting `List<Double>`.

### Test Input
```java
List<Double> prices = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
// Window 1: [10.0, 20.0, 30.0] -> Average: 20.0
// Window 2: [20.0, 30.0, 40.0] -> Average: 30.0
// Window 3: [30.0, 40.0, 50.0] -> Average: 40.0
// Expected Output: [20.0, 30.0, 40.0]
```

??? question "Reveal solution"

    ### Solution
    ```java
    package lab.java25boot4.whatsnew.exercises;

    import java.util.List;
    import java.util.stream.Gatherers;

    public class MovingAverageCalculator {

        public static List<Double> calculateMovingAverage(List<Double> prices, int windowSize) {
            if (prices == null || prices.size() < windowSize) {
                return List.of();
            }

            return prices.stream()
                    .gather(Gatherers.windowSliding(windowSize))
                    .map(window -> window.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                    .toList();
        }

        public static void main(String[] args) {
            List<Double> prices = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
            List<Double> averages = calculateMovingAverage(prices, 3);
            System.out.println(averages); // [20.0, 30.0, 40.0]
        }
    }
    ```

    **Key Points:**
    - `Gatherers.windowSliding(3)` emits overlapping `List<Double>` windows without state leakage.
    - Each window is mapped to its average via standard stream reduction.
    - The operation preserves stream laziness and can safely participate in parallel pipelines.
