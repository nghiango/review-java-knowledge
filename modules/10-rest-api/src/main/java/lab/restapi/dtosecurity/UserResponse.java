package lab.restapi.dtosecurity;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id, String email, String fullName, String role, boolean verified, Instant createdAt) {

    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getRole(),
                entity.isVerified(),
                entity.getCreatedAt());
    }
}
