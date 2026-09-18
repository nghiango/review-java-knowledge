package lab.corejava.broken.optionalerrors;

import java.util.Optional;

public class CustomerProfileService {
    private final ProfileRepository repository;
    private Optional<CustomerProfile> lastProfile = Optional.empty();

    public CustomerProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    public CustomerProfile load(Optional<String> email) {
        try {
            var profile = repository.findByEmail(email.get()).get();
            lastProfile = Optional.of(profile);
            return profile;
        } catch (Exception ignored) {
            return null;
        }
    }

    public Optional<CustomerProfile> getLastProfile() {
        return lastProfile;
    }
}
