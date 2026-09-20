package lab.springcore.questions;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/** Q04: Demonstrates Spring Stereotype Annotations (@Component, @Service, @Repository). */
@SuppressWarnings("unused")
public class Q04SpringStereotypesExample {

    @Component
    static class GenericWorker {}

    @Service
    static class DomainService {}

    @Repository
    static class JpaEntityRepository {}

    public static void main(String[] args) {
        boolean isComponent = GenericWorker.class.isAnnotationPresent(Component.class); // true
        boolean isService = DomainService.class.isAnnotationPresent(Service.class); // true
        boolean isRepository =
                JpaEntityRepository.class.isAnnotationPresent(Repository.class); // true
    }
}
