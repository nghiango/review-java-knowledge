package lab.jvm.examples;

public final class ContainerMemoryDemo {
    private ContainerMemoryDemo() {}

    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        System.out.printf("maxMemoryMiB=%d%n", runtime.maxMemory() / (1024 * 1024));
        System.out.printf("availableProcessors=%d%n", runtime.availableProcessors());
    }
}
