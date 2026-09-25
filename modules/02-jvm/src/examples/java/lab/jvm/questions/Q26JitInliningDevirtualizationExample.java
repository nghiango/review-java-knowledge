package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q26JitInliningDevirtualizationExample {
    private Q26JitInliningDevirtualizationExample() {}

    public interface Formatter {
        String format(String input);
    }

    public static class JsonFormatter implements Formatter {
        @Override
        public String format(String input) {
            return "{\"val\":\"" + input + "\"}";
        }
    }

    public static class XmlFormatter implements Formatter {
        @Override
        public String format(String input) {
            return "<val>" + input + "</val>";
        }
    }

    public static class CsvFormatter implements Formatter {
        @Override
        public String format(String input) {
            return "\"" + input + "\",";
        }
    }

    public static String executeFormat(Formatter formatter, String text) {
        // Monomorphic call site (1 receiver type seen by C2):
        // C2 devirtualizes and inlines target method directly via class guard check.
        // Bimorphic call site (2 receiver types):
        // C2 generates conditional branch (if instanceof JsonFormatter else XmlFormatter).
        // Megamorphic call site (>= 3 receiver types):
        // Inline cache bails out; reverts to full vtable lookup via invokevirtual, causing inlining
        // failure!
        return formatter.format(text);
    }

    public static void main(String[] args) {
        Formatter f1 = new JsonFormatter();
        String out1 = executeFormat(f1, "hello"); // "{\"val\":\"hello\"}"
    }
}
