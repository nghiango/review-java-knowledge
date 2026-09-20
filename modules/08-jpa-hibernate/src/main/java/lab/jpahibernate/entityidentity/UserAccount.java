package lab.jpahibernate.entityidentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String displayName;

    protected UserAccount() {}

    public UserAccount(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Fixed equals & hashCode using natural business key (email). Handles Hibernate proxies and
     * null safety correctly.
     */
    @Override
    @SuppressWarnings("EqualsGetClass")
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass =
                o instanceof HibernateProxy proxy
                        ? proxy.getHibernateLazyInitializer().getPersistentClass()
                        : o.getClass();
        Class<?> thisEffectiveClass =
                this instanceof HibernateProxy proxy
                        ? proxy.getHibernateLazyInitializer().getPersistentClass()
                        : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserAccount that = (UserAccount) o;
        return email != null && Objects.equals(email, that.email);
    }

    @Override
    public final int hashCode() {
        return email != null ? Objects.hash(email) : getClass().hashCode();
    }
}
