package lab.jpahibernate.broken.cascademapping;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EntityManager entityManager;

    public EnrollmentService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void unregisterStudent(Long studentId) {
        Student student = entityManager.find(Student.class, studentId);
        if (student != null) {
            entityManager.remove(student);
        }
    }
}
