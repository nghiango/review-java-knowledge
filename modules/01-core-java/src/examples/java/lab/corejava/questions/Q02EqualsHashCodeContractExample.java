package lab.corejava.questions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public final class Q02EqualsHashCodeContractExample {
    private Q02EqualsHashCodeContractExample() {}

    public record AccountKey(String tenantId, String accountNumber) {
        public AccountKey {
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(accountNumber);
        }
    }

    public static void main(String[] args) {
        AccountKey key1 = new AccountKey("tenant-1", "acc-99");
        AccountKey key2 = new AccountKey("tenant-1", "acc-99");

        boolean equal = key1.equals(key2); // true
        boolean sameHash = (key1.hashCode() == key2.hashCode()); // true (contract satisfied)

        Map<AccountKey, String> balances = new HashMap<>();
        balances.put(key1, "$500");
        String lookup = balances.get(key2); // "$500" (lookup succeeds in same bucket)
    }
}
