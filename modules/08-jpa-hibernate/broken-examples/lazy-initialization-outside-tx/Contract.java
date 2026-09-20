package lab.jpahibernate.broken.lazyinit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id private String id;
    private String contractNumber;
    private double value;

    public Contract() {}

    public Contract(String id, String contractNumber, double value) {
        this.id = id;
        this.contractNumber = contractNumber;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public double getValue() {
        return value;
    }
}
