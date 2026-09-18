package lab.jvm.examples;

public final class StackDepthDemo {
    private static final String CONFIRMATION = "--i-understand";

    private StackDepthDemo() {}

    public static void main(String[] args) {
        if (!confirmed(args)) {
            System.out.printf("Usage: java %s %s%n", StackDepthDemo.class.getName(), CONFIRMATION);
            return;
        }
        try {
            recurse(0);
        } catch (StackOverflowError error) {
            System.out.println("stack overflow reached; rerun with -Xss to compare stack depth");
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private static int recurse(int depth) {
        // Intentional diagnostic recursion for comparing -Xss stack depth in an opt-in demo.
        return recurse(depth + 1) + depth;
    }

    private static boolean confirmed(String[] args) {
        return args.length == 1 && CONFIRMATION.equals(args[0]);
    }
}
