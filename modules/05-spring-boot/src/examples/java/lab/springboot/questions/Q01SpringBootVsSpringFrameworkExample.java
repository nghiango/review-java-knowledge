package lab.springboot.questions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class Q01SpringBootVsSpringFrameworkExample {

    @SpringBootApplication
    static class SampleApplication {}

    public static void main(String[] args) {
        // Spring Framework requires explicit DispatcherServlet, view resolvers, and web server
        // setup.
        // Spring Boot packages opinionated auto-configuration and embedded runtime in a single
        // bootstrap call.
        SpringApplication app = new SpringApplication(SampleApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        ConfigurableApplicationContext ctx = app.run(args);
        boolean isRunning = ctx.isRunning(); // true
        String[] beanNames = ctx.getBeanDefinitionNames();
        boolean hasBeans = beanNames.length > 0; // true

        System.out.println("Spring Boot running: " + isRunning + ", beans: " + hasBeans);
        ctx.close();
    }
}
