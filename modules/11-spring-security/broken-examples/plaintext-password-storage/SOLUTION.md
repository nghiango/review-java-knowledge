# Solution: Weak Password Hashing & Insecure Verification

## Annotated Code

### `InsecureRegistrationController.java`
```java
package lab.springsecurity.broken.passwords;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class InsecureRegistrationController {

    private final Map<String, UserAccount> userDatabase = new ConcurrentHashMap<>();

    // Security issue: Storing passwords using broken cryptographic hash (MD5) without unique salt or adaptive work factor (CWE-328 / CWE-916).
    // Security issue: Missing input validation on password length and complexity, permitting empty or single-character passwords.
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationDto dto) {
        String hashedPassword = hashWithMd5(dto.password());
        UUID id = UUID.randomUUID();
        UserAccount account = new UserAccount(id, dto.username(), hashedPassword, "ROLE_USER");
        userDatabase.put(dto.username(), account);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    // Security issue: Non-constant-time comparison (String.equals) leaks character-by-character timing information, enabling side-channel password hash recovery.
    // Architecture issue: Manual authentication logic implemented in controller instead of using Spring Security's AuthenticationManager and PasswordEncoder.
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto dto) {
        UserAccount account = userDatabase.get(dto.username());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        String inputHash = hashWithMd5(dto.password());
        if (account.storedPassword().equals(inputHash)) {
            return ResponseEntity.ok("Login successful");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

    private String hashWithMd5(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }

    public record RegistrationDto(String username, String password) {}
    public record LoginDto(String username, String password) {}
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Broken / Unsalted MD5 Hash Algorithm | Security | Critical | MD5 is cryptographically broken and fast to compute. Without random salt per password and tunable key-stretching work factors (e.g. BCrypt, Argon2, PBKDF2), attacker rainbow tables and GPU cracking can reverse hashes in seconds (CWE-328, CWE-916). |
| Non-Constant-Time Password Comparison | Security | Major | Comparing secret hashes using `String.equals()` terminates on the first mismatched byte, creating measurable timing discrepancies that attackers can exploit to guess hashes byte-by-byte (CWE-208). |
| Missing Input Validation & Complexity Rules | Security | Major | Endpoints accept null or arbitrary strings without enforcing minimum length (e.g. `@Size(min = 8)`), character sets, or non-blank checks (CWE-521). |
| Ad-Hoc Controller Authentication | Architecture | Major | Handcrafting user lookup and password comparison in REST controllers bypasses Spring Security's `UserDetailsService`, `PasswordEncoder`, and `AuthenticationProvider` infrastructure. |

## Correct Implementation Reference
- [`SafePasswordEncoderConfig.java`](../../src/main/java/lab/springsecurity/passwords/SafePasswordEncoderConfig.java)
- [`UserRegistrationService.java`](../../src/main/java/lab/springsecurity/passwords/UserRegistrationService.java)
- [`SafeRegistrationController.java`](../../src/main/java/lab/springsecurity/passwords/SafeRegistrationController.java)
