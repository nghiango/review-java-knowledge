package lab.jpahibernate.questions;

import java.util.HashSet;
import java.util.Set;

public class Q13EntityEqualsHashCode {

    // Demonstrating the pitfall of auto-generated ID in hashCode()
    static class BrokenAccount {
        private Long id; // null before persist, populated upon flush/persist

        public BrokenAccount(Long id) {
            this.id = id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenAccount that)) return false;
            return id != null && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

    public static void main(String[] args) {
        BrokenAccount account = new BrokenAccount(null); // Transient
        Set<BrokenAccount> set = new HashSet<>();
        set.add(account);

        // Simulated persist assigns generated DB id
        account.setId(42L);

        // Lookup fails because hashCode changed from 0 to 42, placing object in the wrong hash
        // bucket!
        boolean foundAfterIdAssigned = set.contains(account); // false

        System.out.println(
                "Found in HashSet after ID assignment: "
                        + foundAfterIdAssigned); // Found in HashSet after ID assignment: false
    }
}
