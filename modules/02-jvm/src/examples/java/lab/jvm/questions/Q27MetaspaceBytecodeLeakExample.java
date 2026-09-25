package lab.jvm.questions;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q27MetaspaceBytecodeLeakExample {
    private Q27MetaspaceBytecodeLeakExample() {}

    public interface TaskHandler {
        void execute();
    }

    public static void main(String[] args) {
        // Metaspace stores JVM class metadata, method bytecode representations, constant pools, and
        // annotations.
        // It resides in native memory (-XX:MetaspaceSize, -XX:MaxMetaspaceSize).
        // A class can only be unloaded if its defining ClassLoader is unreachable and collected.

        List<Object> proxyList = new ArrayList<>();

        // Creating dynamic proxies or CGLIB enhancers using newly allocated ClassLoaders
        // creates Metaspace metadata entries that cannot be collected as long as instances exist.
        for (int i = 0; i < 5; i++) {
            TaskHandler proxy =
                    (TaskHandler)
                            Proxy.newProxyInstance(
                                    TaskHandler.class.getClassLoader(),
                                    new Class<?>[] {TaskHandler.class},
                                    (p, method, mArgs) -> null);
            proxyList.add(proxy);
        }

        int count = proxyList.size(); // 5
        // If unbounded bytecode generation occurs across short-lived dynamic classloaders without
        // GC:
        // java.lang.OutOfMemoryError: Metaspace is thrown!
    }
}
