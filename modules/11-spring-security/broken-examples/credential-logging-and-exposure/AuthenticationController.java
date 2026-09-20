package lab.springsecurity.broken.sanitization;

import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());

    @PostMapping("/token")
    public ResponseEntity<String> authenticate(@RequestBody AuthRequest request) {
        // Logs plaintext password to system logging streams
        LOGGER.info("Authenticating user: " + request.username() + " with password: " + request.password());

        if ("admin".equals(request.username()) && "secret123".equals(request.password())) {
            return ResponseEntity.ok("token_eyJhbGciOi...");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials for: " + request.username() + "/" + request.password());
    }

    public record AuthRequest(String username, String password) {}
}
