package lab.designpatterns.questions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q14: Flyweight Pattern & Object Interning. Demonstrates sharing intrinsic immutable state to
 * support large numbers of fine-grained objects.
 */
public class Q14FlyweightPatternInterningExample {

    public record GlyphStyle(String fontFamily, int fontSize, boolean bold) {}

    public static class GlyphStyleFactory {
        private static final Map<String, GlyphStyle> CACHE = new ConcurrentHashMap<>();

        public static GlyphStyle getStyle(String font, int size, boolean bold) {
            String key = font + ":" + size + ":" + bold;
            return CACHE.computeIfAbsent(key, k -> new GlyphStyle(font, size, bold));
        }

        public static int getCacheSize() {
            return CACHE.size();
        }
    }

    @SuppressWarnings("ReferenceEquality")
    public static void main(String[] args) {
        GlyphStyle style1 = GlyphStyleFactory.getStyle("Arial", 12, true);
        GlyphStyle style2 = GlyphStyleFactory.getStyle("Arial", 12, true);

        boolean sharedReference = (style1 == style2); // true (flyweight shared instance)
        boolean singleAllocation = (GlyphStyleFactory.getCacheSize() == 1); // true

        System.out.println("Q14 shared: " + sharedReference + ", singleAlloc: " + singleAllocation);
    }
}
