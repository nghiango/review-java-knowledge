package lab.java25boot4.springsecurity.broken.chainingdsl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @GetMapping
    public ResponseEntity<String> listAllUsers() {
        return ResponseEntity.ok("ADMIN_USER_LIST: [root, sec_ops]");
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable String userId) {
        return ResponseEntity.ok("USER_DEACTIVATED: " + userId);
    }
}
