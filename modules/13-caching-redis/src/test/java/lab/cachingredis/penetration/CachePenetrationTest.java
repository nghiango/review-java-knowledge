package lab.cachingredis.penetration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CachePenetrationTest {

    private SafeUserProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new SafeUserProfileService();
    }

    @Test
    @DisplayName("Querying existing user hits database once and caches result")
    void existingUser_hitsDatabaseOnce() {
        int initialQueries = profileService.getDatabaseQueryCount();

        UserProfile profile = profileService.getUserProfile(1001L);
        assertThat(profile).isNotNull();
        assertThat(profile.fullName()).isEqualTo("Alice Smith");
        assertThat(profileService.getDatabaseQueryCount()).isEqualTo(initialQueries + 1);

        // Subsequent query hits cache
        UserProfile cached = profileService.getUserProfile(1001L);
        assertThat(cached).isEqualTo(profile);
        assertThat(profileService.getDatabaseQueryCount()).isEqualTo(initialQueries + 1);
    }

    @Test
    @DisplayName(
            "Querying non-existent user caches sentinel and prevents repeated database queries")
    void nonExistentUser_cachesSentinelAndShieldsDatabase() {
        int initialQueries = profileService.getDatabaseQueryCount();

        // First call for non-existent ID queries DB and records sentinel
        UserProfile firstAttempt = profileService.getUserProfile(9999L);
        assertThat(firstAttempt).isNull();
        assertThat(profileService.getDatabaseQueryCount()).isEqualTo(initialQueries + 1);

        // 10 subsequent queries for the non-existent ID hit the sentinel without touching the DB!
        for (int i = 0; i < 10; i++) {
            UserProfile subsequent = profileService.getUserProfile(9999L);
            assertThat(subsequent).isNull();
        }
        assertThat(profileService.getDatabaseQueryCount()).isEqualTo(initialQueries + 1);

        // If the user is registered, sentinel is evicted immediately
        profileService.createUserProfile(
                new UserProfile(9999L, "charlie@example.com", "Charlie Brown"));
        UserProfile newlyCreated = profileService.getUserProfile(9999L);
        assertThat(newlyCreated).isNotNull();
        assertThat(newlyCreated.fullName()).isEqualTo("Charlie Brown");
    }
}
