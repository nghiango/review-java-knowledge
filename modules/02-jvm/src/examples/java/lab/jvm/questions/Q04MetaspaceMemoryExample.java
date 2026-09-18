package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q04MetaspaceMemoryExample {
    private Q04MetaspaceMemoryExample() {}

    public static void main(String[] args) {
        // Class metadata (bytecode, constant pool, field descriptors) resides in native Metaspace
        Class<?> clazz = Q04MetaspaceMemoryExample.class;
        String className = clazz.getName(); // "lab.jvm.questions.Q04MetaspaceMemoryExample"
        int methodCount =
                clazz.getDeclaredMethods().length; // 1 (metadata stored in off-heap Metaspace)
    }
}
