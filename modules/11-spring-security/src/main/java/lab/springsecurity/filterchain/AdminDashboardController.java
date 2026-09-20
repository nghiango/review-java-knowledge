package lab.springsecurity.filterchain;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminDashboardController {

    @GetMapping("/admin/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        return ResponseEntity.ok(
                Map.of(
                        "activeSessions", 1420,
                        "dbConnections", 48,
                        "heapUsedMb", 1024,
                        "adminAuditLogCount", 98320));
    }

    @GetMapping("/public/status")
    public ResponseEntity<Map<String, String>> getPublicStatus() {
        return ResponseEntity.ok(Map.of("status", "UP", "version", "2.4.0"));
    }
}
