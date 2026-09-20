package lab.jpahibernate.questions;

public class Q11OwningSideAndMappedBy {

    public static void main(String[] args) {
        // In bidirectional JPA associations:
        // The OWNING side declares @JoinColumn and physically controls the foreign key column in
        // DB.
        // The INVERSE side declares mappedBy = "..." and is a read-only mirror of the association.
        boolean owningSideControlsForeignKey = true; // true
        boolean modifyingInverseSideAloneUpdatesForeignKey =
                false; // false (Hibernate only checks owning side when generating SQL)

        System.out.println(
                "Owning side controls FK: "
                        + owningSideControlsForeignKey); // Owning side controls FK: true
        System.out.println(
                "Inverse side alone updates FK: "
                        + modifyingInverseSideAloneUpdatesForeignKey); // Inverse side alone updates
        // FK: false
    }
}
