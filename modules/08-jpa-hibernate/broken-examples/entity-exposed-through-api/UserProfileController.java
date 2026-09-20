package lab.jpahibernate.broken.dtomapping;

import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final EntityManager entityManager;

    public UserProfileController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfile> getUser(@PathVariable Long id) {
        UserProfile user = entityManager.find(UserProfile.class, id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<UserProfile> updateUser(
            @PathVariable Long id, @RequestBody UserProfile request) {
        UserProfile user = entityManager.find(UserProfile.class, id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setAdmin(request.isAdmin());

        return ResponseEntity.ok(user);
    }
}
