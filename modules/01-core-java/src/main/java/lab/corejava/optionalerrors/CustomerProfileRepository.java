package lab.corejava.optionalerrors;

import java.util.Optional;

@FunctionalInterface
public interface CustomerProfileRepository {
    Optional<CustomerProfile> findByEmail(String email);
}
