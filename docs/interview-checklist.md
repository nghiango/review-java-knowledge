# Interview Checklist

## Core Java

- [ ] Explain OOP, SOLID and composition versus inheritance with trade-offs
- [ ] Define the `equals` / `hashCode` contract and identify mutable-key failures
- [ ] Explain HashMap hashing, collisions, resize and treeification
- [ ] Compare records, immutable classes, sealed types and enums
- [ ] Apply PECS and explain type erasure
- [ ] Distinguish checked and unchecked exceptions and use try-with-resources
- [ ] Use Optional only where absence is part of a return contract
- [ ] Explain stream laziness, stateful operations and collectors
- [ ] Identify unsafe stream side effects and parallel-stream misuse
- [ ] Discuss Java 21 pattern matching and sequenced collections

## JVM & Performance

- [ ] Explain JVM runtime data areas, heap generations, Metaspace and stack frames
- [ ] Trace class loading lifecycle, parent delegation and class initialization triggers
- [ ] Explain JIT tiered compilation (C1, C2), deoptimization and escape analysis
- [ ] Contrast G1, ZGC, Shenandoah and Parallel GC with pause-time and throughput trade-offs
- [ ] Identify GC roots, safepoints, card tables and remembered sets
- [ ] Diagnose `OutOfMemoryError` variants (Heap, Metaspace, Direct, Native Threads)
- [ ] Analyze heap dump dominator trees and JFR allocation profiles
- [ ] Account for container memory limits, `-XX:MaxRAMPercentage` and cgroup limits

## Related

- [Roadmap](roadmap.md)
- [Java questions](questions/java.md)

