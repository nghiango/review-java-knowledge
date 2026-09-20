package lab.springcore.questions;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/** Q09: Demonstrates BeanPostProcessor vs BeanFactoryPostProcessor. */
@SuppressWarnings("unused")
public class Q09BppVsBfppExample {

    // BFPP: Operates on BeanDefinition metadata BEFORE any bean is instantiated
    static class CustomBfpp implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
                throws BeansException {
            int definitionCount = beanFactory.getBeanDefinitionCount();
        }
    }

    // BPP: Operates on bean instances AFTER instantiation, before/after initialization
    static class CustomBpp implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName)
                throws BeansException {
            return bean; // Intercept or wrap instances with proxies
        }
    }

    public static void main(String[] args) {
        boolean bfppModifiesMetadata = true; // true
        boolean bppModifiesInstances = true; // true
    }
}
