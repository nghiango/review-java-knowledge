package lab.corejava.questions;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unused", "UnnecessaryAsync"})
public final class Q13ConcurrentHashMapExample {
    private Q13ConcurrentHashMapExample() {}

    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> counter = new ConcurrentHashMap<>();
        counter.put("requests", 10);

        // Atomic compound update on bucket node using CAS / synchronized node header
        int updated = counter.compute("requests", (k, v) -> v == null ? 1 : v + 1); // 11

        int finalCount = counter.get("requests"); // 11
    }
}
