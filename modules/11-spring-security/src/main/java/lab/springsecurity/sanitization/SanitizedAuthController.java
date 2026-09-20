package lab.springsecurity.sanitization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SanitizedAuthController {

    private static final Logger LOGGER = Logger.getLogger(SanitizedAuthController.class.getName());

    @PostMapping("/token")
    public ResponseEntity<String> authenticate(@Valid @RequestBody AuthRequest request) {
        // Logs only username, never password
        LOGGER.info("Authentication attempt for user: " + request.username());

        if ("admin".equals(request.username()) && "secret123".equals(request.password())) {
            return ResponseEntity.ok("token_eyJhbGciOi...");
        }

        // Generic error message without echoing credentials
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

    public record AuthRequest(@NotBlank String username, @NotBlank String password) {}
}
