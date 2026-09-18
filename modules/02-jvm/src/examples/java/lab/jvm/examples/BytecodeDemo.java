package lab.jvm.examples;

public final class BytecodeDemo {
    private BytecodeDemo() {}

    public static int score(int left, int right) {
        int total = left + right;
        if (total > 10) {
            return total * 2;
        }
        return total - 1;
    }

    public static int parseOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static void main(String[] args) {
        System.out.printf("score=%d%n", score(4, 9));
        System.out.printf("parsed=%d%n", parseOrDefault(args.length == 0 ? "21" : args[0], -1));
    }
}
