package lab.jpahibernate.broken.lazyinit;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id private String id;
    private String name;

    @OneToMany(fetch = FetchType.LAZY)
    private List<Contract> contracts = new ArrayList<>();

    public Customer() {}

    public Customer(String id, String name, List<Contract> contracts) {
        this.id = id;
        this.name = name;
        this.contracts = contracts;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Contract> getContracts() {
        return contracts;
    }
}
