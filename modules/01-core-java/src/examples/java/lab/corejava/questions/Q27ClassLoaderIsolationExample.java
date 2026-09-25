package lab.corejava.questions;

import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public final class Q27ClassLoaderIsolationExample {
    private Q27ClassLoaderIsolationExample() {}

    public static void main(String[] args) throws Exception {
        ClassLoader appLoader = Q27ClassLoaderIsolationExample.class.getClassLoader();

        // Two distinct ClassLoaders loading from the same classpath
        URL[] urls = new URL[] {
            Q27ClassLoaderIsolationExample.class.getProtectionDomain().getCodeSource().getLocation()
        };

        try (URLClassLoader customLoader1 = new URLClassLoader(urls, null);
             URLClassLoader customLoader2 = new URLClassLoader(urls, null)) {

            Class<?> classFromLoader1 = customLoader1.loadClass(Q27ClassLoaderIsolationExample.class.getName());
            Class<?> classFromLoader2 = customLoader2.loadClass(Q27ClassLoaderIsolationExample.class.getName());

            // Even though the bytecode and class names are identical:
            boolean sameClass = (classFromLoader1 == classFromLoader2); // false (different initiating ClassLoaders)
            boolean assignable = classFromLoader1.isAssignableFrom(classFromLoader2); // false

            // Attempting to cast an instance from loader1 into loader2's type:
            // throws ClassCastException: cannot be cast to class ... in loader2
        }
    }
}
