package lab.jvm.questions;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public final class Q17RedeployClassLoaderLeakExample {
    private Q17RedeployClassLoaderLeakExample() {}

    public static void main(String[] args) throws Exception {
        URLClassLoader pluginLoader = new URLClassLoader(new URL[0]);
        WeakReference<ClassLoader> ref = new WeakReference<>(pluginLoader);

        // If a static field in a parent classloader holds a reference to a plugin class or object,
        // pluginLoader cannot be garbage-collected, retaining all plugin classes in Metaspace.
        pluginLoader.close();
        pluginLoader = null;
        boolean isTracked =
                (ref.get() == null); // false until all strong reference chains to the loader are
        // severed
    }
}
