## Collections

The Java Collections Framework is a standardized set of interfaces, implementations, and algorithms for storing and manipulating groups of objects.

![Collections Hierarchy](images/collections-hierachy.png)

The main interfaces include `List`, `Set`, `Queue`, `Deque`, and `Map`.

- `List` maintains an ordered sequence and allows duplicates.
- `Set` ensures uniqueness of elements.
- `Queue` and `Deque` support processing elements in different orders.
- `Map` associates keys with values.

Collection is the root interface for most collection types, while Collections is a utility class that provides algorithms such as sorting and searching. Map belongs to the framework but does not extend Collection.

Collection extends `Iterable` and defines common operations such as add, remove, contains, size, clear, and iterator.

Collection itself does not define indexed access or guarantee ordering. These behaviors depend on the subinterface and implementation.

In practice, I prefer programming to interfaces. If a method only needs general collection operations, I use Collection. If it requires indexed access, I use List. If it requires key-value pairs, I use Map. This reduces coupling to concrete implementations and makes the code easier to maintain.

### Removing while iterating

This is a common interview trap

```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
for (String s : list) {
    if (s.equals("b")) {
        list.remove(s); // ConcurrentModificationException
    }
}

// Good
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
list.removeIf(s -> s.equals("b"));

// Or through iterator
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String s = iterator.next();
    if (s.equals("b")) {
        iterator.remove();
    }
}
```

### List

`List<E>` extends `Collection<E>` and represents an ordered sequence of elements.

Its main characteristics are:

- Positional ordering: Each element has an index starting at 0.
- Duplicates allowed: Multiple elements can be equal.
- Indexed operations: Access, insert, replace, and remove elements by position.
- Null support: Many implementations allow null elements, but not all.
- Interation order: Iteration follows the list's positional order.

#### ArrayList internals

`ArrayList` is backed by a dynamically resized array. By default its capacity is 10, and it doubles when resized.

If there is available capacity, the new element is stored in the next slot.

When the array becomes full, the implementation allocates a larger array and copies the existing references.

This means:

- `get(index)` is O(1) because elements are stored in an array
- Appending is O(1) amortized
- Inserting or removing near the beginning requires shifting elements, making it O(n)

#### LinkedList internals

`LinkedList` is backed by a doubly-linked list. Each element is a node that contains a reference to the next and previous nodes.

This means:

- `get(index)` is O(n) because elements are stored in a linked list
- Appending is O(1)
- Inserting or removing requires traversing the list to find the position, making it O(n). However, inserting or removing the first or last element is O(1).

#### ArrayList vs LinkedList performance

LinkedList insertion is O(1) only when the position is already known, such as at the ends or though a positioned ListIterator.

Even when two operations have the same Big-O complexity, ArrayList often performs better because its references are stored contiguously, improving cache locality.

LinkedList requires following references between separate node objects and usually consumes more memory.

#### sublist()

`sublist(fromIndex, toIndex)` returns a view backed by the original list, not an independent copy.

```java

List<String> names =
    new ArrayList<>(List.of("A", "B", "C", "D"));

List<String> subset = names.subList(1, 3);

subset.set(0, "X");

System.out.println(names);
// [A, X, C, D]
```

Change through the view can affect the original list.

#### Mutable vs immutable vs unmodifiable lists

- Mutable lists can be modified after creation
- Immutable lists cannot be modified after creation
- Unmodifiable lists cannot be modified after creation. However, it is just only a wrapper, if original list mutable, and we change the original list, the unmodifiable list will also change.

```java

 List<String> mutableList = new ArrayList<>();
        mutableList.add("A");
        mutableList.add("B");
        System.out.println(mutableList); // [A, B]

        // 2. UNMODIFIABLE LIST (A wrapper view around the mutable list)
        List<String> unmodifiableList = Collections.unmodifiableList(mutableList);
        System.out.println(unmodifiableList); // [A, B]

        // 3. IMMUTABLE LIST (A true independent snapshot)
        List<String> immutableList = List.copyOf(mutableList);
        System.out.println(immutableList ); // [A, B]


        mutableList.add("C");
        // It will throw errors
        // unmodifiableList.add("D")
        System.out.println(unmodifiableList); // [A, B, C]

        // It will throw errors
        // immutableList.add("D")
        System.out.println(immutableList); // [A, B]

```

#### Choosing the right List

- Use `ArrayList` for indexed access, iteration, appending, and most general-purpose list operations.
- Use `LinkedList` when frequent insertions and removals happen through a positioned iterator or when its combined List and Deque functionality is genuinely needed.
- Use `ImmutableList` when you need an immutable snapshot that won't change
- Use `UnmodifiableList` when you need a read-only view of a mutable list
- Use `ArrayDeque` for most single-threaded queue or deque workloads, ArrayDeque ir preferable to LinkedList.
- Use specialized collections for concurrency like `ConcurrentHashMap`, `CopyOnWriteArrayList`, etc. Other workloads may need synchronization or different concurrent data structure.

### Set

`Set<E>` extends `Collection<E>` and represents a collection that does not contain duplicate elements.

Its main characteristics are:

- Uniqueness: A Set cannot contain two elements considered equal under its equality rules.
- No indexed access: Unlike List, it does not provide get(index).
- Ordering depends on implementation: Some sets are unordered, some preserve insertion order, and others sort elements.
- Null handling depends on implementation.
- Not inherently thread-safe.

Hash-based operations are not guaranteed constant-time in every case; collisions and resizing affect performance. TreeSet provides log-time performance for basic operations.

The Set<E> interface has several implementations:

- HashSet: Hash-based, no ordering, O(1) average performance. The internal implementation is HashMap.
- LinkedHashSet: Hash-based with insertion ordering, O(1) average performance. The internal implementation is LinkedHashMap. It's good to use when deduplicating data while preserving the order in which elements first appeared.
- TreeSet: Tree-based, sorted, O(log n) performance. The internal implementation is a TreeMap, which uses a self-balancing red-black tree. TreeSet is especially useful when you need sorted interation or navigation methods such as `first()`, `last()`, `floor()`, and `ceiling()`.

#### TreeSet: Comparable vs Comparator

TreeSet needs a way to order its elements

- `Comparable<T>` - Natural ordering defined by the element type itself

```java

record Product(long id, int price)
        implements Comparable<Product> {

    @Override
    public int compareTo(Product other) {
        int byPrice = Integer.compare(
            this.price, other.price
        );

        if (byPrice != 0) {
            return byPrice;
        }

        return Long.compare(this.id, other.id);
    }
}
```

Here, products are ordered by price, then ID

The ID tie-breaker is important. Without it, products with the same price would compare as 0, and TreeSet would treat them as duplicates.

- `Comparator<T>` - Custom ordering provided by the user

```java
Set<Product> products = new TreeSet<>(
    Comparator.comparingInt(Product::price)
              .thenComparingLong(Product::id)
);
```

This allows sorting without requirig the Product class to define natural ordering.

For TreeSet to fully obey the general Set equality contract, the ordering should be consistent with `equals()`

#### Practical use cases

- HashSet: Fast membership checks use for checking whether a value has already been processed, deduplicating identifiers, or tracking visited nodes in algorithms.
- LinkedHashSet: Deduplication with order preservation, useful when you need both uniqueness and insertion order.
- TreeSet: Sorted deduplication with ordering, useful when you need both uniqueness and sorted iteration.

### Map

Map<K, V> represents a collection of key-value mappings.

Unlike List and Set, Map does not extend the Collection interface, although it is part of the Java Collections Framework.

Its main characteristics are:

- Unique keys: Each key maps to at most one value.
- Duplicate values allowed: Different keys may map to the same value.
- Lookup by key: Retrieve values using their associated keys.
- Ordering depends on implementation: Some maps are unordered, insertion-ordered, or sorted by key.
- Null handling depends on implementation.

Map<K, V> has several implementations:

- HashMap: Fast key-value lookups with no ordering guarantees.
- LinkedHashMap: Maintains insertion order or access order for predictable iteration.
- TreeMap: Keeps keys sorted by their natural order or a custom Comparator.

#### HashMap internals

HashMap uses a hash table consisting of buckets.

Each bucket can contain entries with the same bucket index.

In the Java 8+ implementation, treeification is attempted when insertion encounters an existing chain of at least 7 nodes, resulting in a bucket of at least nine nodes. If the table capacity is below 64, HashMap prefers resizing instead of treeifying

![How HashMap works](images/how-hashmap-works.png)

##### How put() works conceptually

1. Calculate the key's hashCode().
2. Spread the hash bits and calculate the bucket index.
3. Check whether an entry with an equal key already exists.
4. Replace the existing value if the key matches.
5. Otherwise, insert a new entry.
6. Resize the table when necessary.

##### How get() works conceptually

1. Calculate the key's hashCode().
2. Spread the hash bits and calculate the bucket index.
3. Traverse the bucket to find an entry with an equal key.
4. Return the associated value if found.
5. Return null if no matching key is found.

#### LinkedHashMap internals

LinkedHashMap extends HashMap and maintains a doubly linked sequence of entries.

This provides predictable iteration order.

##### How it works

- Each entry has links to the previous and next entries.
- The iteration order is determined by the order in which entries were inserted or accessed.
- When `accessOrder` is true, the most recently accessed entry is moved to the end.

#### TreeMap internals

TreeMap uses a red-black tree, a self-balancing binary search tree.

Keys are ordered using either natural ordering (Comparable) or a provided Comparator.

#### Thread-Safe Collections and ConcurrentHashMap Internals

A thread-safe collection makes its documented operations safe for concurrent use, but it does not automatically make every sequence of operations atomic.

| Collection | Thread safety approach | Best use case |
| --- | --- | --- |
| ConcurrentHashMap | CAS, volatile visibility, fine-grained synchronization | Concurrent key-value access |
| Collections.synchronizedMap() | Single wrapper mutex | Simple synchronized map access |
| CopyOnWriteArrayList | Copy backing array on mutation | Read-heavy lists |
| ConcurrentLinkedQueue | Non-blocking CAS-based queue | Unbounded concurrent FIFO |
| ArrayBlockingQueue | Lock-based bounded blocking queue | Producer-consumer workloads |
| LinkedBlockingQueue | Lock-based blocking queue | Producer-consumer workloads |
| ConcurrentSkipListMap | Concurrent ordered skip list | Sorted concurrent map |

##### ConcurrentHashMap internals

ConcurrentHashMap<K, V> is designed for concurrent reads and updates without locking the entire map for every operation.

In Java 8+, it uses an array of bins, similar in broad structure to HashMap.

A bin can contain a linked list or tree nodes.

###### How Java 8+ ConcurrentHashMap works

- Reads: get() normally retrieves entries without acquiring a lock, using volatile visibility and safe publication.
- Insertion into an empty bin: Can use compare-and-swap (CAS).
- Updates to an occupied bin: May synchronize on the bin's first node.
- Heavy collisions: Bins may be represented by tree nodes.
- Resizing: Multiple threads can cooperate in transferring entries to a larger table.

###### Why not lock the entire map?

Locking the whole map for every operation creates unnecessary contention when different threads access unrelated keys.

ConcurrentHashMap allows much more parallelism for many common workloads.

#### CAS(Compare-and-swap) vs synchronized

**CAS** is an atomic CPU-supported operation that conceptually does:

```java
if (expectedValue == currentValue) {
    // Atomically update currentValue to newValue
    currentValue = newValue;
    return true; // Success
} else {
    return false; // Failed - another thread modified the value
}
```

The comparison and update occur atomically, unlike an ordinary Java if statement.

CAS is useful when contention is relatively low and a short atomic update can succeed without acquiring a lock.

**synchronized** is a keyword that provides mutual exclusion by acquiring a lock on an object or class. It ensures that only one thread can execute a block of code or method at a time.

When updating an occupied bin, ConcurrentHashMap may synchronize on the bin's first node to coordinate modifications.

This prevents multiple threads from corrupting that bin's structure.

#### Atomic operations: compute, merge, putIfAbsent

Consider a counter stored in a ConcurrentHashMap.

**Incorrect approach**

```java
ConcurrentHashMap<String, Integer> counts =
    new ConcurrentHashMap<>();

counts.put("Java", 0);

// Not atomic as a combined operation:
counts.put("Java", counts.get("Java") + 1);
```

Each map method is individually thread-safe, but the overall read-modify-write sequence is not atomic.

**Correct approach A: compute()**

```java
ConcurrentHashMap<String, Integer> counts =
    new ConcurrentHashMap<>();

counts.put("Java", 0);

counts.compute("Java", (key, oldValue) ->
    oldValue == null ? 1 : oldValue + 1
);
```

The remapping operation is atomic for the specified key.

**Correct approach B: merge()**

```java
ConcurrentHashMap<String, Integer> counts =
    new ConcurrentHashMap<>();

counts.merge("Java", 1, Integer::sum);
counts.merge("Java", 1, Integer::sum);

System.out.println(counts.get("Java")); // 2
```

merge() is convenient for counters and aggregation.

**Correct approach C: LongAdder**

For frequently updated counters, use LongAdder.

```java
ConcurrentHashMap<String, LongAdder> counts =
    new ConcurrentHashMap<>();

counts.computeIfAbsent(
    "Java",
    key -> new LongAdder()
).increment();

long total = counts.get("Java").sum();
```

LongAdder distributes updates across internal cells under contention, reducing competition for a single variable.

However, sum() is not an atomic snapshot with respect to concurrent updates.
