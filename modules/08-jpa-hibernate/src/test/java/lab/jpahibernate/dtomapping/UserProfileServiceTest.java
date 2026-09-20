package lab.jpahibernate.dtomapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    private EntityManager entityManager;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        userProfileService = new UserProfileService(entityManager);
    }

    @Test
    @DisplayName("findUserProfileById returns DTO excluding sensitive password hash")
    void findUserProfileById_returnsDto() {
        @SuppressWarnings("unchecked")
        TypedQuery<UserProfileDto> query = mock(TypedQuery.class);
        UserProfileDto dto = new UserProfileDto(1L, "alice", "alice@example.com", false);

        when(entityManager.createQuery(anyString(), eq(UserProfileDto.class))).thenReturn(query);
        when(query.setParameter(eq("id"), eq(1L))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(dto));

        Optional<UserProfileDto> result = userProfileService.findUserProfileById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("alice");
        assertThat(result.get().email()).isEqualTo("alice@example.com");
        assertThat(result.get().admin()).isFalse();
    }

    @Test
    @DisplayName("updateUserProfile updates only permissible fields, ignoring admin flag")
    void updateUserProfile_updatesPermissibleFieldsOnly() {
        UserProfile user = new UserProfile("alice", "alice@example.com", "hashed_pwd", false);
        when(entityManager.find(UserProfile.class, 1L)).thenReturn(user);

        UpdateUserProfileRequest request =
                new UpdateUserProfileRequest("alice_new", "new_email@example.com");
        Optional<UserProfileDto> result = userProfileService.updateUserProfile(1L, request);

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("alice_new");
        assertThat(result.get().email()).isEqualTo("new_email@example.com");
        assertThat(user.isAdmin()).isFalse(); // admin unchanged
        assertThat(user.getPasswordHash()).isEqualTo("hashed_pwd"); // password unchanged
    }
}
