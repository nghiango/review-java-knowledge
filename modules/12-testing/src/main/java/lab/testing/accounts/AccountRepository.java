package lab.testing.accounts;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Account}.
 *
 * <p>{@code findByEmail} is a derived query and therefore inherits the database's comparison
 * semantics: PostgreSQL matches the {@code email} column case-sensitively. Callers that want to
 * look an account up regardless of how the user typed the address must normalise it first (see
 * {@link AccountService}) rather than expecting the query to be case-insensitive.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);
}
