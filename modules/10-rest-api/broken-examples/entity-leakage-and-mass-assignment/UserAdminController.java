package lab.restapi.broken.dtosecurity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final Map<Long, UserAccount> userDatabase = new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUser(@PathVariable Long id) {
        UserAccount user = userDatabase.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // Exposing entity directly leaks passwordHash and internal balance
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> updateUser(
            @PathVariable Long id, @RequestBody UserAccount payload) {
        UserAccount existingUser = userDatabase.get(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }

        // Direct binding allows mass assignment of role and accountBalance
        existingUser.setUsername(payload.getUsername());
        existingUser.setEmail(payload.getEmail());
        existingUser.setRole(payload.getRole());
        existingUser.setAccountBalance(payload.getAccountBalance());

        return ResponseEntity.ok(existingUser);
    }
}
