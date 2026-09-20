package lab.springboot.questions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class Q04ConditionalAnnotationsExample {

    record RedisCacheClient(String host) {}

    @Configuration
    static class CacheAutoConfiguration {

        @Bean
        @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
        @ConditionalOnProperty(
                prefix = "app.cache",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true)
        @ConditionalOnMissingBean
        public RedisCacheClient redisCacheClient() {
            return new RedisCacheClient("localhost:6379");
        }
    }

    public static void main(String[] args) {
        CacheAutoConfiguration config = new CacheAutoConfiguration();
        RedisCacheClient client = config.redisCacheClient();

        String host = client.host(); // "localhost:6379"
        boolean isDefaultHost = host.contains("6379"); // true

        System.out.println(
                "Cache client initialized with host: " + host + ", matches: " + isDefaultHost);
    }
}
