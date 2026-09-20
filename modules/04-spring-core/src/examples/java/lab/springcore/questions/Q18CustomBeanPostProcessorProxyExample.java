package lab.springcore.questions;

import java.lang.reflect.Proxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/** Q18: Demonstrates custom BeanPostProcessor dynamically proxying beans. */
@SuppressWarnings("unused")
public class Q18CustomBeanPostProcessorProxyExample {

    interface Auditable {
        void execute();
    }

    @Component
    static class CustomProxyBpp implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName)
                throws BeansException {
            if (bean instanceof Auditable target) {
                // Dynamically wrap matching bean instances with a proxy
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        bean.getClass().getInterfaces(),
                        (proxy, method, args) -> {
                            return method.invoke(target, args);
                        });
            }
            return bean;
        }
    }

    public static void main(String[] args) {
        boolean customBppCanWrapProxies = true; // true
    }
}
