package lab.jvm.questions;

import java.util.Locale;

@SuppressWarnings("unused")
public final class Q12TieredCompilationDeoptExample {
    private Q12TieredCompilationDeoptExample() {}

    public interface Formatter {
        String format(String s);
    }

    public static class Upper implements Formatter {
        @Override
        public String format(String s) {
            return s.toUpperCase(Locale.ROOT);
        }
    }

    public static class Lower implements Formatter {
        @Override
        public String format(String s) {
            return s.toLowerCase(Locale.ROOT);
        }
    }

    public static String process(Formatter f, String s) {
        return f.format(s); // C2 assumes monomorphic call site; deoptimizes if new subtype appears
    }

    public static void main(String[] args) {
        Formatter f1 = new Upper();
        String r1 = process(f1, "hello"); // "HELLO"
        Formatter f2 = new Lower();
        String r2 = process(f2, "HELLO"); // "hello" (triggers deoptimization trap and recompiles as
        // polymorphic)
    }
}
