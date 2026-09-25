package lab.springcore.questions;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

@SuppressWarnings("unused")
public final class Q25ConditionalPhaseEvaluationExample {
    private Q25ConditionalPhaseEvaluationExample() {}

    // ConfigurationCondition provides phase-aware condition evaluation:
    // PARSE_CONFIGURATION: Evaluated while parsing @Configuration classes (before bean definitions exist)
    // REGISTER_BEAN: Evaluated when registering bean definitions (allows inspecting other bean definitions)
    public static class OnDatabaseAvailableCondition implements ConfigurationCondition {
        @Override
        public ConfigurationPhase getConfigurationPhase() {
            // Evaluated during bean definition registration phase so we can inspect existing bean definitions
            return ConfigurationPhase.REGISTER_BEAN;
        }

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // Safe to check if a specific DataSource bean has been defined:
            return context.getBeanFactory() != null
                && context.getBeanFactory().containsBeanDefinition("dataSource");
        }
    }

    public static void main(String[] args) {
        ConfigurationCondition condition = new OnDatabaseAvailableCondition();
        ConfigurationCondition.ConfigurationPhase phase = condition.getConfigurationPhase(); // REGISTER_BEAN
        boolean isRegisterPhase = (phase == ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN); // true
    }
}
