package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q18: Decorator Ordering & Security Context Propagation. Demonstrates that Outer Authorization
 * Decorator guarantees security checks precede cache hits.
 */
public class Q18DecoratorOrderingSecurityExample {

    public interface SecretService {
        String getSecret(String key, String role);
    }

    public static class RealSecretService implements SecretService {
        @Override
        public String getSecret(String key, String role) {
            return "CONFIDENTIAL_DATA_FOR:" + key;
        }
    }

    // Inner Decorator: Caching
    public static class CachingSecretDecorator implements SecretService {
        private final SecretService delegate;
        private final Map<String, String> cache = new HashMap<>();

        public CachingSecretDecorator(SecretService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getSecret(String key, String role) {
            return cache.computeIfAbsent(key, k -> delegate.getSecret(k, role));
        }
    }

    // Outer Decorator: Authorization
    public static class AuthorizingSecretDecorator implements SecretService {
        private final SecretService delegate;

        public AuthorizingSecretDecorator(SecretService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getSecret(String key, String role) {
            if (!"ADMIN".equals(role)) {
                throw new SecurityException("Forbidden");
            }
            return delegate.getSecret(key, role);
        }
    }

    public static void main(String[] args) {
        // Secure pipeline: Auth is outside Caching!
        SecretService target = new RealSecretService();
        SecretService caching = new CachingSecretDecorator(target);
        SecretService securePipeline = new AuthorizingSecretDecorator(caching);

        // Admin call populates cache
        String adminAccess =
                securePipeline.getSecret("doc-1", "ADMIN"); // "CONFIDENTIAL_DATA_FOR:doc-1"

        // Guest call cannot bypass auth even with cache hit!
        boolean blockedGuest = false;
        try {
            securePipeline.getSecret("doc-1", "GUEST");
        } catch (SecurityException e) {
            blockedGuest = true; // true
        }

        System.out.println("Q18 admin: " + adminAccess + ", blockedGuest: " + blockedGuest);
    }
}
