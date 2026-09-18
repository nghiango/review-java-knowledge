# JVM Exercises

Hands-on diagnostic and analysis exercises. Attempt each problem before expanding the solution.

---

## Exercise 1: Heap Dump Dominator Analysis

### Scenario
An e-commerce backend service begins throwing `OutOfMemoryError: Java heap space` during Black Friday promotions. You obtain a heap dump from the crashed container.

Inspecting the MAT Dominator Tree shows:
- 78% of the total retained heap is held by an instance of `java.util.concurrent.ConcurrentHashMap$Node[]`.
- The shortest path to GC roots reveals a static reference in `com.example.catalog.SessionPricingRegistry`.
- Each entry in the map contains a `PricingSession` object holding a `byte[] payload` (size ~256KB) and a user UUID.

### Question
1. What is the root cause of the memory exhaustion?
2. What immediate operational mitigation would you apply?
3. How would you redesign `SessionPricingRegistry` for long-term production resilience?

??? success "Reveal solution"
    1. **Root cause:** An unbounded static cache (`SessionPricingRegistry`) storing high-cardinality session keys with heavy payloads (~256KB each). Under high promotional traffic, thousands of concurrent sessions filled the heap because no maximum capacity, weight bounding, or TTL eviction was enforced.
    2. **Immediate mitigation:** Increase container memory and `-Xmx` temporarily if host headroom allows; otherwise restart instances with a hotfix capping session registration or disabling payload caching.
    3. **Long-term redesign:**
       - Replace `ConcurrentHashMap` with a bounded cache (e.g. Caffeine) with explicit `maximumWeight` and `expireAfterWrite(15, TimeUnit.MINUTES)`.
       - Expose cache size and eviction metrics via Micrometer to Grafana.
       - Store only essential identifiers in memory and offload large binary payloads to Redis or external object storage.

---

## Exercise 2: Class Initialization and Bytecode Analysis

### Scenario
Consider the class initialization sequence in `ClassInitializationDemo.java`:

```java
--8<-- "modules/02-jvm/src/examples/java/lab/jvm/examples/ClassInitializationDemo.java"
```

### Question
1. What is the exact printed sequence when `ClassInitializationDemo.main()` executes?
2. Explain the JVM mechanism behind the execution order of parent vs child `<clinit>` static initializers.
3. How does the bytecode for `message()` connect to the superclass method?

??? success "Reveal solution"
    1. **Output:**
       ```text
       main starts
       Parent static initializer
       Child static initializer
       parent ready, child ready
       ```
    2. **Explanation:**
       - When `main()` invokes `Child.message()`, the JVM must first ensure `Child` is initialized.
       - Per JVM Specification §5.5, before a class is initialized, its direct superclass (`Parent`) must be initialized first.
       - Therefore, `Parent.<clinit>()` executes first, printing `"Parent static initializer"`, followed by `Child.<clinit>()` printing `"Child static initializer"`.
    3. **Bytecode connection:**
       - `Child.message()` generates `invokestatic lab.jvm.examples.ClassInitializationDemo$Parent.parentMessage:()Ljava/lang/String;` to call the parent static method.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Solutions](solutions.md)
