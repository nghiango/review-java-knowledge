package lab.jpahibernate.broken.generatedid;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AccountRegistryService {

    private final Set<UserAccount> pendingAccounts = new HashSet<>();

    public void registerAccount(UserAccount account) {
        // Step 1: Add transient account (id == null) to HashSet (hashCode evaluates based on null id)
        pendingAccounts.add(account);

        // Step 2: Simulate entity persist where database generates identity ID (id is mutated to 101L)
        account.setId(101L);

        // Step 3: HashSet.contains(account) now fails because hashCode changed from 0 to 101!
        boolean exists = pendingAccounts.contains(account);
        if (!exists) {
            // Account is lost in the Set!
            throw new IllegalStateException("Account lost in pending set due to hashCode mutation");
        }
    }

    public Set<UserAccount> getPendingAccounts() {
        return pendingAccounts;
    }
}
