package lab.springcore.questions;

import java.lang.reflect.Proxy;

/** Q11: Demonstrates JDK Dynamic Proxies (interface-based) vs CGLIB (class subclassing). */
@SuppressWarnings("unused")
public class Q11JdkDynamicVsCglibProxyExample {

    interface GreetingService {
        String greet(String name);
    }

    static class GreetingServiceImpl implements GreetingService {
        @Override
        public String greet(String name) {
            return "Hello, " + name;
        }
    }

    public static void main(String[] args) {
        GreetingService target = new GreetingServiceImpl();

        // JDK Dynamic Proxy requires an interface
        GreetingService jdkProxy =
                (GreetingService)
                        Proxy.newProxyInstance(
                                GreetingService.class.getClassLoader(),
                                new Class<?>[] {GreetingService.class},
                                (proxy, method, methodArgs) -> {
                                    return method.invoke(target, methodArgs) + " [PROXIED]";
                                });

        String result = jdkProxy.greet("World"); // "Hello, World [PROXIED]"
        boolean isProxyClass = Proxy.isProxyClass(jdkProxy.getClass()); // true
    }
}
