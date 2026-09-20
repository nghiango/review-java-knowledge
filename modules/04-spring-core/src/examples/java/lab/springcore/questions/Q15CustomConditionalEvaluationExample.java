package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Q15: Demonstrates custom @Conditional and Condition evaluation. */
@SuppressWarnings("unused")
public class Q15CustomConditionalEvaluationExample {

    static class FeatureFlagCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // Evaluates environment property or condition
            return true;
        }
    }

    static class ConditionalFeature {}

    @Configuration
    static class FeatureConfig {
        @Bean
        @Conditional(FeatureFlagCondition.class)
        ConditionalFeature conditionalFeature() {
            return new ConditionalFeature();
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(FeatureConfig.class)) {
            boolean loaded = context.containsBean("conditionalFeature"); // true
        }
    }
}
