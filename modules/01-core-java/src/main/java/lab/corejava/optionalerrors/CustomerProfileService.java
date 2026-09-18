package lab.corejava.optionalerrors;

import java.util.Locale;
import java.util.Objects;

public final class CustomerProfileService {
    private final CustomerProfileRepository repository;

    public CustomerProfileService(CustomerProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public CustomerProfile requireProfile(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        var normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        return repository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomerNotFoundException(normalizedEmail));
    }
}
