package lab.springsecurity.passwords;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserAccount> userDatabase = new ConcurrentHashMap<>();

    public UserRegistrationService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount registerUser(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (userDatabase.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Salted, adaptive key-stretching hash
        String encodedPassword = passwordEncoder.encode(rawPassword);
        UUID id = UUID.randomUUID();
        UserAccount account = new UserAccount(id, username, encodedPassword, "ROLE_USER");
        userDatabase.put(username, account);
        return account;
    }

    public boolean authenticate(String username, String rawPassword) {
        UserAccount account = userDatabase.get(username);
        if (account == null) {
            return false;
        }
        // Constant-time verification preventing timing side-channel attacks
        return passwordEncoder.matches(rawPassword, account.passwordHash());
    }

    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(userDatabase.get(username));
    }
}
