package lab.springcore.questions;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

@SuppressWarnings("unused")
public final class Q28SpringAotReflectionHintsExample {
    private Q28SpringAotReflectionHintsExample() {}

    public static class DynamicPayload {
        private String eventType;

        public String getEventType() {
            return eventType;
        }
    }

    // Ahead-of-Time (AOT) RuntimeHintsRegistrar:
    // In GraalVM native image compilation, closed-world assumption eliminates reflection by
    // default.
    // Spring Framework 6 uses RuntimeHints to explicitly register reflective classes, resources,
    // and JDK dynamic proxies generated during build time.
    public static class CustomPayloadHintsRegistrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Registers DynamicPayload for reflective constructor and getter access:
            hints.reflection()
                    .registerType(
                            DynamicPayload.class,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                            MemberCategory.INVOKE_PUBLIC_METHODS);
        }
    }

    public static void main(String[] args) {
        RuntimeHints hints = new RuntimeHints();
        CustomPayloadHintsRegistrar registrar = new CustomPayloadHintsRegistrar();
        registrar.registerHints(hints, Q28SpringAotReflectionHintsExample.class.getClassLoader());

        // Verifies reflection hints were registered
        boolean hasHints = hints.reflection().getTypeHint(DynamicPayload.class) != null; // true
    }
}
