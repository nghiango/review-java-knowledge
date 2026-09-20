package lab.cachingredis;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lab.testsupport.SharedRedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class RedisIntegrationTest {

    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, String> connection;
    private static RedisCommands<String, String> syncCommands;

    @BeforeAll
    static void startRedis() {
        GenericContainer<?> redisContainer = SharedRedisContainer.instance();
        String host = redisContainer.getHost();
        int port = redisContainer.getFirstMappedPort();

        RedisURI redisUri = RedisURI.Builder.redis(host, port).build();
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        syncCommands = connection.sync();
    }

    @AfterAll
    static void closeRedis() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    @Test
    @DisplayName("Redis container executes string SET and GET with TTL expiration")
    void redisBasicOperations_setAndGetWithTtl() {
        String key = "test:user:101";
        String value = "{\"name\":\"Alice\",\"status\":\"ACTIVE\"}";

        syncCommands.set(key, value, SetArgs.Builder.ex(30));

        String retrieved = syncCommands.get(key);
        assertThat(retrieved).isEqualTo(value);

        Long ttl = syncCommands.ttl(key);
        assertThat(ttl).isGreaterThan(0L).isLessThanOrEqualTo(30L);
    }

    @Test
    @DisplayName("Redis atomic SETNX PX reliably implements distributed locking")
    void redisDistributedLock_setnxEnsuresMutualExclusion() {
        String lockKey = "lock:invoice:inv-888";
        String workerA = "worker-thread-A";
        String workerB = "worker-thread-B";

        // Worker A acquires lock with 5 second lease
        String lockResultA = syncCommands.set(lockKey, workerA, SetArgs.Builder.nx().px(5000));
        assertThat(lockResultA).isEqualTo("OK");

        // Worker B tries to acquire the same lock while A holds it
        String lockResultB = syncCommands.set(lockKey, workerB, SetArgs.Builder.nx().px(5000));
        assertThat(lockResultB).isNull(); // Mutual exclusion: B is rejected!

        // Worker A releases lock
        syncCommands.del(lockKey);

        // Now Worker B can acquire the lock
        String lockResultBRetry = syncCommands.set(lockKey, workerB, SetArgs.Builder.nx().px(5000));
        assertThat(lockResultBRetry).isEqualTo("OK");

        syncCommands.del(lockKey);
    }
}
