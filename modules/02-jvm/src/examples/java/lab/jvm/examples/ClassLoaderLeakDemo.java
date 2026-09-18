package lab.jvm.examples;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public final class ClassLoaderLeakDemo {
    private static final String CONFIRMATION = "--i-understand";
    private static final List<ClassLoader> RETAINED_LOADERS = new ArrayList<>();

    private ClassLoaderLeakDemo() {}

    public static void main(String[] args) throws Exception {
        if (!confirmed(args)) {
            System.out.printf(
                    "Usage: java %s %s%n", ClassLoaderLeakDemo.class.getName(), CONFIRMATION);
            return;
        }
        WeakReference<ClassLoader> released = createReleasedLoader();
        WeakReference<ClassLoader> retained = createRetainedLoader();
        System.out.printf("released loader visible before GC=%s%n", released.get() != null);
        System.out.printf("retained loader visible before GC=%s%n", retained.get() != null);
        System.out.println("GC is nondeterministic; use jcmd/JFR/heap dumps to confirm retention.");
    }

    private static WeakReference<ClassLoader> createReleasedLoader() {
        URLClassLoader loader =
                new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        return new WeakReference<>(loader);
    }

    private static WeakReference<ClassLoader> createRetainedLoader() {
        URLClassLoader loader =
                new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        RETAINED_LOADERS.add(loader);
        return new WeakReference<>(loader);
    }

    private static boolean confirmed(String[] args) {
        return args.length == 1 && CONFIRMATION.equals(args[0]);
    }
}
