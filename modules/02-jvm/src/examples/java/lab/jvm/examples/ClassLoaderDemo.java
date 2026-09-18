package lab.jvm.examples;

public final class ClassLoaderDemo {
    private ClassLoaderDemo() {}

    public static void main(String[] args) {
        printLoader("java.lang.String", String.class);
        printLoader("ClassLoaderDemo", ClassLoaderDemo.class);
        printChain(Thread.currentThread().getContextClassLoader());
    }

    private static void printLoader(String name, Class<?> type) {
        ClassLoader loader = type.getClassLoader();
        String loaderName =
                loader == null ? "bootstrap loader (represented as null)" : loader.toString();
        System.out.printf("%s -> %s%n", name, loaderName);
    }

    private static void printChain(ClassLoader start) {
        System.out.println("context loader chain:");
        for (ClassLoader loader = start; loader != null; loader = loader.getParent()) {
            System.out.printf("  %s%n", loader);
        }
        System.out.println("  bootstrap loader (represented as null)");
    }
}
