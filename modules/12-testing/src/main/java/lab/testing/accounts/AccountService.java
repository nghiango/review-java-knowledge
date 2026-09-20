package lab.testing.accounts;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers accounts and reads their balance.
 *
 * <p>The service, not the database, owns case-insensitive uniqueness. It normalises every email
 * (trim + lower-case) before it is stored or queried, so two addresses that differ only by case
 * resolve to the same account even though PostgreSQL's unique index on {@code email} is
 * case-sensitive and {@code findByEmail} matches case-sensitively. That makes the rule explicit and
 * portable instead of an accident of whichever store is in use.
 *
 * <p>The duplicate check is a read-then-write; it is protected here by the unique index, which
 * makes the losing writer of a concurrent registration fail with a constraint violation rather than
 * create a second row.
 */
@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers a new account for {@code email}, rejecting an address that is already taken once
     * both are normalised.
     *
     * @throws DuplicateEmailException if an account already exists for the normalised email
     */
    @Transactional
    public Account register(String email, String displayName) {
        String normalisedEmail = normalise(email);
        if (repository.findByEmail(normalisedEmail).isPresent()) {
            throw new DuplicateEmailException(normalisedEmail);
        }
        return repository.save(new Account(normalisedEmail, displayName, 0L));
    }

    /**
     * Returns the persisted balance for {@code email}.
     *
     * @throws AccountNotFoundException if no account exists for the normalised email
     */
    @Transactional(readOnly = true)
    public long balance(String email) {
        String normalisedEmail = normalise(email);
        return repository
                .findByEmail(normalisedEmail)
                .map(Account::getBalanceCents)
                .orElseThrow(() -> new AccountNotFoundException(normalisedEmail));
    }

    /** Trim and lower-case an address so lookups do not depend on how the user typed it. */
    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Thrown when an email is already registered. */
    public static final class DuplicateEmailException extends RuntimeException {

        public DuplicateEmailException(String email) {
            super("email already registered: " + email);
        }
    }

    /** Thrown when no account exists for an email. */
    public static final class AccountNotFoundException extends RuntimeException {

        public AccountNotFoundException(String email) {
            super("no account for email: " + email);
        }
    }
}
