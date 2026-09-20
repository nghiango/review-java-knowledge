package lab.jpahibernate.entityidentity;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountRegistryService {

    private final EntityManager entityManager;
    private final Set<UserAccount> activeAccounts = new HashSet<>();

    public AccountRegistryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public UserAccount registerAccount(String email, String displayName) {
        UserAccount account = new UserAccount(email, displayName);

        // Add to Set BEFORE persist
        activeAccounts.add(account);

        entityManager.persist(account);

        // Account is still found in Set AFTER persist because hashCode and equals use stable
        // natural key
        return account;
    }

    public boolean isAccountRegistered(UserAccount account) {
        return activeAccounts.contains(account);
    }

    public int getRegisteredAccountCount() {
        return activeAccounts.size();
    }
}
