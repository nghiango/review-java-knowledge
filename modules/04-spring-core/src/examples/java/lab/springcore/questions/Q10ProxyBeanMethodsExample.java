package lab.springcore.questions;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Q10: Demonstrates @Configuration(proxyBeanMethods = true) vs Lite Mode. */
@SuppressWarnings({"unused", "ReferenceEquality"})
public class Q10ProxyBeanMethodsExample {

    static class DatabaseConnection {}

    static class Repository {
        private final DatabaseConnection connection;

        Repository(DatabaseConnection connection) {
            this.connection = connection;
        }

        public DatabaseConnection getConnection() {
            return connection;
        }
    }

    // proxyBeanMethods = true (default): Intermethod @Bean calls are intercepted to return the same
    // singleton
    @Configuration(proxyBeanMethods = true)
    static class FullConfig {
        @Bean
        DatabaseConnection dbConnection() {
            return new DatabaseConnection();
        }

        @Bean
        Repository repoA() {
            return new Repository(dbConnection());
        }

        @Bean
        Repository repoB() {
            return new Repository(dbConnection());
        }
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(FullConfig.class)) {
            Repository a = context.getBean("repoA", Repository.class);
            Repository b = context.getBean("repoB", Repository.class);

            boolean sameConnection =
                    (a.getConnection() == b.getConnection()); // true (CGLIB intercepted)
        }
    }
}
