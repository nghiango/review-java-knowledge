package lab.jpahibernate.cascademapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollmentServiceTest {

    private EntityManager entityManager;
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        enrollmentService = new EnrollmentService(entityManager);
    }

    @Test
    @DisplayName(
            "unregisterStudent removes student and cascades to join entities, leaving courses intact")
    void unregisterStudent_removesStudentEntity() {
        Student student = new Student("Bob");
        Course course = new Course("Computer Science 101");
        student.enroll(course);

        when(entityManager.find(Student.class, 1L)).thenReturn(student);

        enrollmentService.unregisterStudent(1L);

        verify(entityManager).find(Student.class, 1L);
        verify(entityManager).remove(student);
        assertThat(student.getEnrollments()).hasSize(1);
    }
}
