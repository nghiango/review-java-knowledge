package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Q16: Demonstrates Spring Environment, PropertySource, and active profiles. */
@SuppressWarnings("unused")
public class Q16EnvironmentPropertySourceExample {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            ConfigurableEnvironment env = context.getEnvironment();
            env.setActiveProfiles("production");
            env.getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "testProperties",
                                    java.util.Map.of("app.database.pool-size", "50")));
            context.refresh();

            String poolSize = env.getProperty("app.database.pool-size"); // "50"
            boolean isProd =
                    env.acceptsProfiles(
                            org.springframework.core.env.Profiles.of("production")); // true
        }
    }
}
