package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import lab.testing.accounts.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AccountService}.
 *
 * <p>Runs without Docker: {@link InMemoryAccountRepository} stands in for the database so the suite
 * is fast and self-contained.
 */
class AccountServiceTest {

    private InMemoryAccountRepository repository;
    private AccountService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        service = new AccountService(repository);
    }

    @Test
    void register_thenBalanceWithDifferentCase_findsTheAccount() {
        service.register("ada@example.com", "Ada");

        assertEquals(0L, service.balance("ADA@EXAMPLE.COM"));
    }

    @Test
    void register_secondAddressDifferingOnlyByCase_isRejected() {
        service.register("ada@example.com", "Ada");

        assertThrows(
                IllegalStateException.class,
                () -> service.register("Ada@Example.com", "Ada Lovelace"));
    }

    @Test
    void accounts_returnsThemInTheOrderTheyWereRegistered() {
        service.register("zoe@example.com", "Zoe");
        service.register("ada@example.com", "Ada");

        assertEquals(
                List.of("zoe@example.com", "ada@example.com"),
                service.accounts().stream().map(Account::getEmail).toList());
    }

    @Test
    void save_sameAddressTwice_keepsOnlyOneAccount() {
        repository.save(new Account("ada@example.com", "Ada", 0L));
        repository.save(new Account("ADA@EXAMPLE.COM", "Ada Lovelace", 500L));

        assertEquals(1, repository.findAll().size());
        assertEquals(
                500L, repository.findByEmail("ada@example.com").orElseThrow().getBalanceCents());
    }
}
