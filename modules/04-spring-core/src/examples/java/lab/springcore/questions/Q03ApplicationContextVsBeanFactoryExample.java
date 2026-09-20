package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Q03: Demonstrates ApplicationContext vs BeanFactory. */
@SuppressWarnings("unused")
public class Q03ApplicationContextVsBeanFactoryExample {

    static class SampleBean {}

    public static void main(String[] args) {
        // ApplicationContext eager-initializes singletons and supports AOP/events/i18n
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(SampleBean.class);
            context.refresh();

            boolean containsBean =
                    context.containsBean(
                            "q03ApplicationContextVsBeanFactoryExample.SampleBean"); // true
            SampleBean bean = context.getBean(SampleBean.class); // Non-null managed instance
            boolean isActive = context.isActive(); // true
        }
    }
}
