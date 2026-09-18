package lab.corejava.optionalerrors;

import java.util.Objects;

public record CustomerProfile(String customerId, String email, String displayName) {

    public CustomerProfile {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(displayName, "displayName");
    }
}
