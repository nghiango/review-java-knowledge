package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lab.testing.accounts.Account;

/**
 * In-memory stand-in for the account repository used by the account tests.
 *
 * <p>Keeps the accounts in a map so the tests do not need a database, and keys them by a lower-cased
 * email so that {@code ADA@example.com} and {@code ada@example.com} are treated as the same address.
 */
public final class InMemoryAccountRepository {

    private final Map<String, Account> byEmail = new LinkedHashMap<>();

    public Account save(Account account) {
        byEmail.put(account.getEmail().toLowerCase(Locale.ROOT), account);
        return account;
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(byEmail.get(email.toLowerCase(Locale.ROOT)));
    }

    public List<Account> findAll() {
        return new ArrayList<>(byEmail.values());
    }

    public void deleteAll() {
        byEmail.clear();
    }
}
