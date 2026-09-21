package lab.java25boot4.restapi.questions;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Q04: How do JSpecify nullability annotations (@NullMarked and @Nullable) strengthen REST DTO
 * contracts in Spring Boot 4?
 */
public class Q04JSpecifyRestContractsExample {

    public record UserProfileResponse(
            String userId, String username, @Nullable String phoneNumber, @Nullable String bio) {

        public UserProfileResponse {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(username, "username must not be null");
        }
    }

    public static void main(String[] args) {
        var profile = new UserProfileResponse("USR-42", "alex", null, "Senior Platform Engineer");

        System.out.println("User ID: " + profile.userId()); // "USR-42"
        System.out.println("Username: " + profile.username()); // "alex"
        System.out.println("Phone: " + profile.phoneNumber()); // null
        System.out.println("Has bio: " + (profile.bio() != null)); // true
    }
}
