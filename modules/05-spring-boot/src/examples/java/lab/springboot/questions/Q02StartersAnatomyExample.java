package lab.springboot.questions;

import java.util.List;

public class Q02StartersAnatomyExample {

    record StarterDescriptor(
            String artifactId, List<String> transitiveDependencies, boolean hasCode) {}

    public static void main(String[] args) {
        // A Spring Boot Starter is an aggregate dependency descriptor with curated BOM versions
        StarterDescriptor webStarter =
                new StarterDescriptor(
                        "spring-boot-starter-web",
                        List.of("spring-webmvc", "spring-boot-starter-tomcat", "jackson-databind"),
                        false);

        boolean isAggregatorOnly =
                !webStarter.hasCode(); // true (starters contain pom/gradle metadata, no Java code)
        int depCount = webStarter.transitiveDependencies().size(); // 3

        System.out.println("Starter is aggregator: " + isAggregatorOnly + ", deps: " + depCount);
    }
}
