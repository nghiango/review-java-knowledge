package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import java.util.List;
import lab.testing.accounts.Account;

/**
 * Registers accounts and reads their balance on top of {@link InMemoryAccountRepository}.
 *
 * <p>The address is stored and looked up exactly as the caller supplied it; the repository's
 * lower-cased key is what makes two spellings of the same address resolve to one account.
 */
public final class AccountService {

    private final InMemoryAccountRepository repository;

    public AccountService(InMemoryAccountRepository repository) {
        this.repository = repository;
    }

    public Account register(String email, String displayName) {
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("email already registered: " + email);
        }
        return repository.save(new Account(email, displayName, 0L));
    }

    public long balance(String email) {
        return repository
                .findByEmail(email)
                .map(Account::getBalanceCents)
                .orElseThrow(() -> new IllegalStateException("no account for email: " + email));
    }

    public List<Account> accounts() {
        return repository.findAll();
    }
}
