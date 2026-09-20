package lab.springsecurity.passwords;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SafeRegistrationController {

    private final UserRegistrationService registrationService;

    public SafeRegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody RegistrationRequest request) {
        UserAccount account =
                registrationService.registerUser(request.username(), request.password());
        URI location = URI.create("/api/users/" + account.id());
        return ResponseEntity.created(location)
                .body(
                        new UserResponseDto(
                                account.id().toString(), account.username(), account.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        boolean valid = registrationService.authenticate(request.username(), request.password());
        if (valid) {
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    public record RegistrationRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters long")
                    String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record UserResponseDto(String id, String username, String role) {}
}
