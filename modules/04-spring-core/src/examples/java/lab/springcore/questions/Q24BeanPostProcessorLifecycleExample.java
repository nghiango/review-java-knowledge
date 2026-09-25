package lab.springcore.questions;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

@SuppressWarnings("unused")
public final class Q24BeanPostProcessorLifecycleExample {
    private Q24BeanPostProcessorLifecycleExample() {}

    public static class SampleService {
        private String state = "raw";

        public void init() {
            this.state = state + " -> initialized";
        }

        public String getState() {
            return state;
        }
    }

    // BeanPostProcessor intercepts bean initialization lifecycle:
    // 1. postProcessBeforeInitialization (e.g. @PostConstruct, ApplicationContextAware injection)
    // 2. InitializingBean.afterPropertiesSet() / custom init-method
    // 3. postProcessAfterInitialization (e.g. AOP proxy creation, transactional proxy wrapping)
    public static class CustomAuditingPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName)
                throws BeansException {
            if (bean instanceof SampleService s) {
                s.state = s.state + " -> before-init";
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName)
                throws BeansException {
            if (bean instanceof SampleService s) {
                s.state = s.state + " -> after-init";
            }
            return bean; // Can return dynamic proxy wrapping the target instance
        }
    }

    public static void main(String[] args) {
        SampleService service = new SampleService();
        CustomAuditingPostProcessor bpp = new CustomAuditingPostProcessor();

        bpp.postProcessBeforeInitialization(service, "sampleService");
        service.init();
        bpp.postProcessAfterInitialization(service, "sampleService");

        String finalState = service.getState(); // "raw -> before-init -> initialized -> after-init"
    }
}
