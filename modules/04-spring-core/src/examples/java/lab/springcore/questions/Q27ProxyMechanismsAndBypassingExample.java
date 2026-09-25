package lab.springcore.questions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.springframework.aop.framework.ProxyFactory;

@SuppressWarnings({"unused", "ReferenceEquality"})
public final class Q27ProxyMechanismsAndBypassingExample {
    private Q27ProxyMechanismsAndBypassingExample() {}

    public interface Calculator {
        int add(int a, int b);
    }

    public static class SimpleCalculator implements Calculator {
        @Override
        public int add(int a, int b) {
            return a + b;
        }

        // Final method in target class: CGLIB CANNOT override or intercept final methods!
        public final int multiply(int a, int b) {
            return a * b; // Silently executed directly without AOP interception in CGLIB!
        }
    }

    public static void main(String[] args) {
        SimpleCalculator target = new SimpleCalculator();

        // Spring ProxyFactory defaults to CGLIB for classes, JDK Dynamic Proxy when interfaces are configured
        ProxyFactory factory = new ProxyFactory(target);
        factory.addInterface(Calculator.class);

        Calculator proxy = (Calculator) factory.getProxy();
        int sum = proxy.add(2, 3); // 5 (intercepted via AOP advice chain)

        // Identity caveat:
        boolean sameReference = (proxy == target); // false (proxy is a separate wrapper instance!)
    }
}
