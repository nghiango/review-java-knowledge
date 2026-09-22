package lab.java25boot4.whatsnew.securitygate;

import java.util.Set;

/**
 * The authenticated caller an authorization decision is made about.
 *
 * <p>Authorization is a property of the caller, not of the code running on the JVM — the model the
 * removed {@code SecurityManager} could never express.
 */
public record Caller(String id, Set<String> authorities) {

    public Caller {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("caller id must not be blank");
        }
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }
}
