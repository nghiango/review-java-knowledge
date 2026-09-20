package lab.springsecurity.filterchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SecurityFilterChainTest {

    private final AdminDashboardController controller = new AdminDashboardController();

    @Test
    @DisplayName("Public status endpoint returns service status")
    void publicStatus_returns200() {
        ResponseEntity<Map<String, String>> response = controller.getPublicStatus();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("Admin metrics returns telemetry data")
    void adminMetrics_returns200() {
        ResponseEntity<Map<String, Object>> response = controller.getSystemMetrics();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("activeSessions");
    }
}
