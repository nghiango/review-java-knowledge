package lab.jpahibernate.entityidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityIdentityTest {

    private EntityManager entityManager;
    private AccountRegistryService registryService;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        registryService = new AccountRegistryService(entityManager);
    }

    @Test
    @DisplayName("Natural key equals and hashCode are stable before and after persist")
    void registerAccount_preservesSetMembership() {
        UserAccount account = registryService.registerAccount("user@example.com", "Alice");

        assertThat(registryService.getRegisteredAccountCount()).isEqualTo(1);
        assertThat(registryService.isAccountRegistered(account)).isTrue();
    }

    @Test
    @DisplayName("Two instances with the same natural key are considered equal")
    void equals_sameNaturalKey_returnsTrue() {
        UserAccount acc1 = new UserAccount("user@example.com", "Alice");
        UserAccount acc2 = new UserAccount("user@example.com", "Alice Updated");

        Set<UserAccount> accounts = new HashSet<>();
        accounts.add(acc1);

        assertThat(accounts.contains(acc2)).isTrue();
        assertThat(acc1).isEqualTo(acc2);
        assertThat(acc1.hashCode()).isEqualTo(acc2.hashCode());
    }
}
