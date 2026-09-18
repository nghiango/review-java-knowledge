package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q01JvmJreJdkExample {
    private Q01JvmJreJdkExample() {}

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version"); // e.g. "21.0.6"
        String vmName = System.getProperty("java.vm.name"); // e.g. "OpenJDK 64-Bit Server VM"
        long maxHeapBytes = Runtime.getRuntime().maxMemory(); // e.g. 4294967296 (4 GB managed heap)
    }
}
