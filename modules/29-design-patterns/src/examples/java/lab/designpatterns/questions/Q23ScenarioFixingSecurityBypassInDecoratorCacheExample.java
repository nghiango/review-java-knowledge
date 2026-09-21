package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q23: Scenario: Resolving Data Leak Caused by Misordered Caching and Authorization Decorators.
 * Demonstrates the secure ordering where authorization decorator envelopes the caching decorator.
 */
public class Q23ScenarioFixingSecurityBypassInDecoratorCacheExample {

    public interface AccountRepository {
        String findAccountData(String accountId, String requesterRole);
    }

    public static class DatabaseAccountRepository implements AccountRepository {
        @Override
        public String findAccountData(String accountId, String requesterRole) {
            return "DATABASE_RECORD:" + accountId;
        }
    }

    public static class CachingAccountDecorator implements AccountRepository {
        private final AccountRepository delegate;
        private final Map<String, String> cache = new HashMap<>();

        public CachingAccountDecorator(AccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public String findAccountData(String accountId, String requesterRole) {
            return cache.computeIfAbsent(
                    accountId, id -> delegate.findAccountData(id, requesterRole));
        }
    }

    public static class AuthorizingAccountDecorator implements AccountRepository {
        private final AccountRepository delegate;

        public AuthorizingAccountDecorator(AccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public String findAccountData(String accountId, String requesterRole) {
            if ("RESTRICTED".equals(accountId) && !"AUDITOR".equals(requesterRole)) {
                throw new SecurityException("Permission denied for restricted account");
            }
            return delegate.findAccountData(accountId, requesterRole);
        }
    }

    public static void main(String[] args) {
        // Enforce correct order: Authorizing wraps Caching
        AccountRepository db = new DatabaseAccountRepository();
        AccountRepository caching = new CachingAccountDecorator(db);
        AccountRepository secureRepo = new AuthorizingAccountDecorator(caching);

        // Auditor reads and populates cache
        String auditorView = secureRepo.findAccountData("RESTRICTED", "AUDITOR");

        // Unauthorized user is blocked by outer authorization layer despite cache hit
        boolean unauthorizedBlocked = false;
        try {
            secureRepo.findAccountData("RESTRICTED", "USER");
        } catch (SecurityException e) {
            unauthorizedBlocked = true; // true
        }

        System.out.println("Q23 auditorView: " + auditorView + ", blocked: " + unauthorizedBlocked);
    }
}
