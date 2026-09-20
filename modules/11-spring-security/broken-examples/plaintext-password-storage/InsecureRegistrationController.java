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

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationDto dto) {
        // Plain MD5 hashing without salt or adaptive work factor
        String hashedPassword = hashWithMd5(dto.password());
        UUID id = UUID.randomUUID();
        UserAccount account = new UserAccount(id, dto.username(), hashedPassword, "ROLE_USER");
        userDatabase.put(dto.username(), account);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto dto) {
        UserAccount account = userDatabase.get(dto.username());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        String inputHash = hashWithMd5(dto.password());
        // Standard String.equals is vulnerable to timing attacks
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
