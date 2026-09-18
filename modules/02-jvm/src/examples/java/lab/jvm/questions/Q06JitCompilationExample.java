package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q06JitCompilationExample {
    private Q06JitCompilationExample() {}

    public static int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        int sum = 0;
        // HotSpot interpreter counts invocations; when threshold is reached, C1/C2 compiles and
        // inlines add()
        for (int i = 0; i < 20_000; i++) {
            sum += add(i, 1);
        }
        int total = sum; // 200010000 (executed via JIT compiled native machine instructions)
    }
}
