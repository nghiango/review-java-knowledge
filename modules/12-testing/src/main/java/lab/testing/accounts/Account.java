package lab.testing.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A registered customer account.
 *
 * <p>Mapped as a JPA entity (not a record) because the persistence provider owns its identity and
 * needs a no-argument constructor. The email carries a unique constraint; because PostgreSQL
 * compares {@code text} case-sensitively, the constraint alone does not give case-insensitive
 * uniqueness — {@link AccountService} normalises the email before saving to enforce that rule
 * explicitly.
 */
@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    /** Required by JPA. */
    protected Account() {}

    public Account(String email, String displayName, long balanceCents) {
        this.email = email;
        this.displayName = displayName;
        this.balanceCents = balanceCents;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getBalanceCents() {
        return balanceCents;
    }
}
