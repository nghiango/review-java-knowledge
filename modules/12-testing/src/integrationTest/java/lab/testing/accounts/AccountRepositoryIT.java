package lab.testing.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestConstructor;

/**
 * Persistence tests for {@link AccountRepository} and {@link AccountService} against a real
 * PostgreSQL container.
 *
 * <p>The point of the slice is that the assertions describe PostgreSQL's semantics rather than an
 * in-memory substitute's: the unique index on {@code email} is case-sensitive, {@code findByEmail}
 * is case-sensitive, and row order is only guaranteed when it is requested with a {@link Sort}.
 * {@link AccountService} therefore has to normalise the email itself — case-insensitive uniqueness
 * is an application rule, not something the database or a fake repository provides.
 *
 * <p>{@code replace = Replace.NONE} stops Boot from swapping the container-backed DataSource for an
 * embedded one, and the container itself comes from {@link TestingJpaConfiguration}. The
 * collaborators are constructor-injected ({@code @TestConstructor}) so the test does not depend on
 * field injection.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TestingJpaConfiguration.class, AccountService.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AccountRepositoryIT {

    private final AccountRepository repository;
    private final AccountService service;
    private final EntityManager entityManager;

    AccountRepositoryIT(
            AccountRepository repository, AccountService service, EntityManager entityManager) {
        this.repository = repository;
        this.service = service;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName(
            "the unique index on email is case-sensitive, so addresses differing only by case both persist")
    void save_emailsDifferingOnlyByCase_bothPersist() {
        repository.saveAndFlush(new Account("ada@example.com", "Ada", 0L));
        repository.saveAndFlush(new Account("Ada@Example.com", "Ada Lovelace", 0L));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName(
            "register normalises the email, so the second address differing only by case is rejected")
    void register_emailDifferingOnlyByCase_isRejected() {
        service.register("ada@example.com", "Ada");

        assertThatThrownBy(() -> service.register("  Ada@Example.com  ", "Ada Lovelace"))
                .isInstanceOf(AccountService.DuplicateEmailException.class);
    }

    @Test
    @DisplayName("findByEmail is case-sensitive, so callers must query with the normalised value")
    void findByEmail_isCaseSensitive() {
        repository.saveAndFlush(new Account("ada@example.com", "Ada", 0L));

        assertThat(repository.findByEmail("ada@example.com")).isPresent();
        assertThat(repository.findByEmail("Ada@Example.com")).isEmpty();
    }

    @Test
    @DisplayName("row order is not insertion order; it must be requested with Sort")
    void findAll_sortedByEmail_returnsRowsInEmailOrder() {
        repository.saveAll(
                List.of(
                        new Account("zoe@example.com", "Zoe", 0L),
                        new Account("ada@example.com", "Ada", 0L),
                        new Account("mia@example.com", "Mia", 0L)));
        repository.flush();

        List<String> emails =
                repository.findAll(Sort.by(Sort.Direction.ASC, "email")).stream()
                        .map(Account::getEmail)
                        .toList();

        assertThat(emails).containsExactly("ada@example.com", "mia@example.com", "zoe@example.com");
    }

    @Test
    @DisplayName(
            "balance reads the persisted row after a flush and clear, not the persistence context")
    void balance_afterFlushAndClear_reflectsPersistedState() {
        repository.saveAndFlush(new Account("ada@example.com", "Ada", 2500L));
        entityManager.clear();

        assertThat(service.balance("  Ada@Example.com  ")).isEqualTo(2500L);
    }
}
