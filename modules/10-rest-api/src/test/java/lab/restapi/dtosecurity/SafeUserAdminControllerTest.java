package lab.restapi.dtosecurity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SafeUserAdminControllerTest {

    private UserService userService;
    private SafeUserAdminController controller;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        controller = new SafeUserAdminController(userService);
    }

    @Test
    @DisplayName("Registration does not leak password hash in response DTO")
    void register_validPayload_doesNotExposePasswordHash() {
        var req = new UserRegistrationRequest("user@example.com", "secret12345", "John Doe");
        ResponseEntity<UserResponse> response = controller.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("user@example.com");
        assertThat(response.getBody().role()).isEqualTo("ROLE_USER");
        assertThat(response.getBody().verified()).isFalse();
    }

    @Test
    @DisplayName(
            "Profile update only updates allowed fields and prevents role privilege escalation")
    void updateProfile_validRequest_updatesNameOnly() {
        var reg = new UserRegistrationRequest("user2@example.com", "secret12345", "Jane Doe");
        UserResponse registered = userService.register(reg);

        ResponseEntity<UserResponse> updated =
                controller.updateProfile(registered.id(), new UserUpdateRequest("Jane SuperAdmin"));

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().fullName()).isEqualTo("Jane SuperAdmin");
        assertThat(updated.getBody().role()).isEqualTo("ROLE_USER"); // Role cannot be mass-assigned
    }
}
