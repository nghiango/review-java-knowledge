package lab.jvm.examples;

public final class GcRootsDemo {
    private static Object staticRoot;

    private GcRootsDemo() {}

    public static void main(String[] args) {
        Object localRoot = new byte[1024];
        staticRoot = localRoot;
        System.out.printf("local root identity=%d%n", System.identityHashCode(localRoot));
        System.out.printf("static root identity=%d%n", System.identityHashCode(staticRoot));
        staticRoot = null;
    }
}
