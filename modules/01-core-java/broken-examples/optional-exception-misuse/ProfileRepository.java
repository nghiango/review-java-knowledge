package lab.corejava.broken.optionalerrors;

import java.util.Optional;

public interface ProfileRepository {
    Optional<CustomerProfile> findByEmail(String email);
}
