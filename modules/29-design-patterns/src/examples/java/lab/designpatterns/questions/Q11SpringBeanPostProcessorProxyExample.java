package lab.designpatterns.questions;

import java.lang.reflect.Proxy;

/**
 * Q11: Dynamic Proxies & Spring BeanPostProcessor Pattern. Demonstrates how Spring wraps beans in
 * dynamic proxies for cross-cutting transactions/metrics.
 */
public class Q11SpringBeanPostProcessorProxyExample {

    public interface OrderService {
        String submitOrder(String id);
    }

    public static class DefaultOrderService implements OrderService {
        @Override
        public String submitOrder(String id) {
            return "ORDER_PROCESSED:" + id;
        }
    }

    public static class TransactionalProxyFactory {
        @SuppressWarnings("unchecked")
        public static <T> T createProxy(T target, Class<T> iface) {
            return (T)
                    Proxy.newProxyInstance(
                            iface.getClassLoader(),
                            new Class<?>[] {iface},
                            (proxy, method, methodArgs) -> {
                                // Simulation of Spring @Transactional interceptor
                                String result = (String) method.invoke(target, methodArgs);
                                return "[TX_COMMITTED]" + result;
                            });
        }
    }

    public static void main(String[] args) {
        OrderService target = new DefaultOrderService();
        OrderService proxy = TransactionalProxyFactory.createProxy(target, OrderService.class);

        String result = proxy.submitOrder("ORD-1"); // "[TX_COMMITTED]ORDER_PROCESSED:ORD-1"
        boolean isProxy = Proxy.isProxyClass(proxy.getClass()); // true

        System.out.println("Q11 result: " + result + ", isProxy: " + isProxy);
    }
}
