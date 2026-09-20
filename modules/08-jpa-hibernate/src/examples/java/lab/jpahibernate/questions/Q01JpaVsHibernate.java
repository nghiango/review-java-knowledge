package lab.jpahibernate.questions;

public class Q01JpaVsHibernate {

    public static void main(String[] args) {
        // JPA is the specification (Jakarta Persistence API: EntityManager, Query, Entity)
        String jpaInterface = "jakarta.persistence.EntityManager";
        // Hibernate is the reference ORM implementation (Session, SessionFactory, StatelessSession)
        String hibernateImpl = "org.hibernate.Session";

        boolean isHibernateJpaProvider = true; // true
        boolean providesStatelessSession = true; // true

        System.out.println(
                "JPA API: " + jpaInterface); // JPA API: jakarta.persistence.EntityManager
        System.out.println("Hibernate: " + hibernateImpl); // Hibernate: org.hibernate.Session
        System.out.println("Is Provider: " + isHibernateJpaProvider); // Is Provider: true
        System.out.println(
                "Stateless Session Support: "
                        + providesStatelessSession); // Stateless Session Support: true
    }
}
