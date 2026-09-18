package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q11TlabAndEscapeAnalysisExample {
    private Q11TlabAndEscapeAnalysisExample() {}

    public record Point(int x, int y) {}

    public static int nonEscapingComputation(int a, int b) {
        // Point instance does not escape method scope.
        // Escape analysis applies scalar replacement: fields 'x' and 'y' allocated in CPU
        // registers/stack!
        Point p = new Point(a, b);
        return p.x() + p.y();
    }

    public static void main(String[] args) {
        int result =
                nonEscapingComputation(10, 20); // 30 (zero heap allocation via scalar replacement)
    }
}
