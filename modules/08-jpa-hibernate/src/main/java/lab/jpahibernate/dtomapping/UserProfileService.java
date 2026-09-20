package lab.jpahibernate.dtomapping;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final EntityManager entityManager;

    public UserProfileService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Projects directly to DTO, ensuring sensitive fields like passwordHash are omitted from the
     * contract.
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> findUserProfileById(Long id) {
        return entityManager
                .createQuery(
                        "SELECT new lab.jpahibernate.dtomapping.UserProfileDto(u.id, u.username, u.email, u.admin) "
                                + "FROM UserProfile u WHERE u.id = :id",
                        UserProfileDto.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    /**
     * Explicitly copies only authorized client-provided fields, preventing Mass Assignment
     * vulnerabilities.
     */
    @Transactional
    public Optional<UserProfileDto> updateUserProfile(Long id, UpdateUserProfileRequest request) {
        UserProfile user = entityManager.find(UserProfile.class, id);
        if (user == null) {
            return Optional.empty();
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        // admin and passwordHash remain strictly unmodifiable via this path

        return Optional.of(
                new UserProfileDto(
                        user.getId(), user.getUsername(), user.getEmail(), user.isAdmin()));
    }
}
