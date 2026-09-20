package lab.concurrency.questions;

import java.util.concurrent.ConcurrentHashMap;

/** Q15: Demonstrates ConcurrentHashMap atomic operations (computeIfAbsent, merge). */
@SuppressWarnings({"unused", "UnnecessaryAsync"})
public class Q15ConcurrentHashMapInternalsExample {

    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // Atomic computeIfAbsent: lock is held only on target bucket node
        int val1 = map.computeIfAbsent("hits", k -> 1); // 1
        int val2 = map.computeIfAbsent("hits", k -> 99); // 1 (already present)

        // Atomic merge: lock-free CAS on empty bucket or synchronized on head node
        int merged = map.merge("hits", 5, Integer::sum); // 6

        int finalValue = map.get("hits"); // 6
    }
}
