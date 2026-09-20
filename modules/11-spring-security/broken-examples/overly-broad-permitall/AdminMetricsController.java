package lab.springsecurity.broken.filterchain;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMetricsController {

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        return ResponseEntity.ok(Map.of(
                "activeSessions", 1420,
                "dbConnections", 48,
                "heapUsedMb", 1024,
                "adminAuditLogCount", 98320));
    }
}
