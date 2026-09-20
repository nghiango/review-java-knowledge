package lab.springboot.questions;

import java.util.Map;

public class Q17DockerComposeIntegrationExample {

    record ComposeServiceBinding(
            String serviceName, String image, Map<String, String> injectedProperties) {}

    public static void main(String[] args) {
        // spring-boot-docker-compose discovers compose.yaml and automatically wires service
        // connections
        ComposeServiceBinding redisService =
                new ComposeServiceBinding(
                        "redis",
                        "redis:7.2-alpine",
                        Map.of(
                                "spring.data.redis.host",
                                "localhost",
                                "spring.data.redis.port",
                                "6379"));

        boolean bindsHost =
                redisService.injectedProperties().containsKey("spring.data.redis.host"); // true
        String host =
                redisService.injectedProperties().get("spring.data.redis.host"); // "localhost"

        System.out.println(
                "Discovered compose service: "
                        + redisService.serviceName()
                        + ", binds host: "
                        + bindsHost
                        + ", host: "
                        + host);
    }
}
