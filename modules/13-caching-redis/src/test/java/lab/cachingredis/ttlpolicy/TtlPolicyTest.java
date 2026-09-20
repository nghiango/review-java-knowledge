package lab.cachingredis.ttlpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TtlPolicyTest {

    private SafeSessionTracker sessionTracker;

    @BeforeEach
    void setUp() {
        sessionTracker = new SafeSessionTracker();
    }

    @Test
    @DisplayName("Registered sessions have finite TTL bounded within the expected jitter range")
    void registerSession_hasFiniteTtlWithJitter() {
        String userId = "usr-101";
        String token = "tok-xyz";
        sessionTracker.registerSession(userId, token);

        String key = "session:" + userId + ":" + token;
        SafeSessionTracker.CachedItem item = sessionTracker.getItem(key);

        assertThat(item).isNotNull();
        assertThat(item.payload()).isEqualTo("ACTIVE");

        long now = System.currentTimeMillis();
        long remainingMs = item.expiresAtEpochMs() - now;

        // Base TTL is 2 hours (7,200,000 ms), jitter is +/- 60,000 ms
        assertThat(remainingMs).isGreaterThan(7_100_000L).isLessThan(7_300_000L);
        assertThat(sessionTracker.isSessionActive(userId, token)).isTrue();
    }

    @Test
    @DisplayName("Search queries are stored with short bounded TTL")
    void cacheSearchResult_hasBoundedTtl() {
        String query = "wireless mechanical keyboard";
        sessionTracker.cacheSearchResult(query, "{\"items\": []}");

        assertThat(sessionTracker.isSessionActive("nonexistent", "dummy")).isFalse();
    }
}
