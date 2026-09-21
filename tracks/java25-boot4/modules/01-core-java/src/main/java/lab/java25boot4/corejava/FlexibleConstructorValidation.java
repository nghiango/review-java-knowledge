package lab.java25boot4.corejava;

import java.time.Instant;
import java.util.Objects;

/**
 * Demonstrates Java 25 Flexible Constructor Bodies (JEP 482). Allows pre-initialization statements
 * and argument validation *before* super(...) execution.
 */
public class FlexibleConstructorValidation {

    public static class BaseEntity {
        private final String id;
        private final Instant createdAt;

        public BaseEntity(String id, Instant createdAt) {
            this.id = Objects.requireNonNull(id, "ID cannot be null");
            this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        }

        public String getId() {
            return id;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }

    public static class UserAccount extends BaseEntity {
        private final String email;

        public UserAccount(String id, String email) {
            // Java 25 Flexible Constructor Bodies: statements before super(...)
            String normalizedId =
                    Objects.requireNonNull(id, "id must not be null").trim().toLowerCase();
            if (normalizedId.isEmpty()) {
                throw new IllegalArgumentException("Normalized ID cannot be empty");
            }
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format: " + email);
            }
            Instant now = Instant.now();

            super(normalizedId, now);

            this.email = email.trim().toLowerCase();
        }

        public String getEmail() {
            return email;
        }
    }
}
