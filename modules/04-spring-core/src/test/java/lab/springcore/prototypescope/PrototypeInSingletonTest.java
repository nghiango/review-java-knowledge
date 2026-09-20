package lab.springcore.prototypescope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PrototypeInSingletonTest {

    @Test
    @DisplayName("ObjectProvider injects a distinct prototype instance for each execution")
    void objectProvider_createsNewPrototypeInstancePerCall() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(ExecutionContext.class, ReportGenerator.class);
            context.refresh();

            ReportGenerator generator = context.getBean(ReportGenerator.class);

            String report1 = generator.generateReport("REQ-AAA");
            String report2 = generator.generateReport("REQ-BBB");

            assertThat(report1).contains("REQ-AAA");
            assertThat(report2).contains("REQ-BBB");
        }
    }
}
