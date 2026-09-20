package lab.restapi.dtosecurity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Map<UUID, UserEntity> users = new ConcurrentHashMap<>();

    public UserResponse register(UserRegistrationRequest request) {
        UUID id = UUID.randomUUID();
        // In production: password would be hashed using Argon2 / BCrypt
        String dummyHash = "hashed_" + request.password();
        UserEntity entity =
                new UserEntity(
                        id,
                        request.email(),
                        dummyHash,
                        request.fullName(),
                        "ROLE_USER", // Defaults to standard role, immune to client tampering
                        false, // Needs email verification
                        Instant.now());
        users.put(id, entity);
        return UserResponse.fromEntity(entity);
    }

    public Optional<UserResponse> findById(UUID id) {
        return Optional.ofNullable(users.get(id)).map(UserResponse::fromEntity);
    }

    public Optional<UserResponse> updateProfile(UUID id, UserUpdateRequest request) {
        return Optional.ofNullable(
                        users.computeIfPresent(
                                id,
                                (key, existing) -> {
                                    existing.setFullName(request.fullName());
                                    return existing;
                                }))
                .map(UserResponse::fromEntity);
    }
}
