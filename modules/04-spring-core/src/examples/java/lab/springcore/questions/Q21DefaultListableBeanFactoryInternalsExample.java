package lab.springcore.questions;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/** Q21: Demonstrates DefaultListableBeanFactory registration and BeanDefinition lifecycle. */
@SuppressWarnings("unused")
public class Q21DefaultListableBeanFactoryInternalsExample {

    static class CoreEngine {
        public String status() {
            return "RUNNING";
        }
    }

    public static void main(String[] args) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        // 1. Programmatically define and register BeanDefinition
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(CoreEngine.class);
        beanDef.setScope("singleton");
        factory.registerBeanDefinition("coreEngine", beanDef);

        // 2. Instantiate and fetch bean from factory
        CoreEngine engine = factory.getBean("coreEngine", CoreEngine.class);
        String status = engine.status(); // "RUNNING"
        boolean isRegistered = factory.containsBean("coreEngine"); // true
    }
}
