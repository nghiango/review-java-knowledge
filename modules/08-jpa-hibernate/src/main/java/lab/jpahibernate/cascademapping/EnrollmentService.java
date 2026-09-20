package lab.jpahibernate.cascademapping;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EntityManager entityManager;

    public EnrollmentService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Correctly unregisters a student by removing the Student entity. Because cascade is applied
     * only to Enrollment (the join entity), only the enrollment links are removed, while Course
     * records remain completely intact.
     */
    @Transactional
    public void unregisterStudent(Long studentId) {
        Student student = entityManager.find(Student.class, studentId);
        if (student != null) {
            entityManager.remove(student);
        }
    }
}
