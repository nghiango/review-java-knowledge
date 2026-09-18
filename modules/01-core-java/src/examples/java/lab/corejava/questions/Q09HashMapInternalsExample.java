package lab.corejava.questions;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class Q09HashMapInternalsExample {
    private Q09HashMapInternalsExample() {}

    public record KeyWithCollision(String name, int fixedHash) {
        @Override
        public int hashCode() {
            return fixedHash; // forces hash collision into same bucket
        }
    }

    public static void main(String[] args) {
        Map<KeyWithCollision, String> map = new HashMap<>();
        KeyWithCollision k1 = new KeyWithCollision("alpha", 42);
        KeyWithCollision k2 = new KeyWithCollision("beta", 42);

        map.put(k1, "first");
        map.put(
                k2,
                "second"); // chained in same bucket linked list (treeifies if bucket >= 8 and table
        // capacity >= 64)

        int size = map.size(); // 2
        String val1 = map.get(k1); // "first" (resolved by equals check inside the bucket)
    }
}
