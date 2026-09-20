# Code Review: Cascade All on ManyToMany Relationship

## Context
A university portal manages courses and student enrollments. When a student unregisters or is purged from the database, the service invokes `entityManager.remove(student)`.

## Code Under Review
- `Student.java` — Student entity declaring `@ManyToMany(cascade = CascadeType.ALL)` to `Course`.
- `Course.java` — Course entity with bidirectional mapping.
- `EnrollmentService.java` — Service method `unregisterStudent(studentId)`.

## Review Questions
1. What catastrophic side effect occurs when `unregisterStudent(Long studentId)` is invoked on a student enrolled in existing courses?
2. Why is `CascadeType.ALL` or `CascadeType.REMOVE` an anti-pattern on `@ManyToMany` associations?
3. How should many-to-many associations and join table records be managed correctly in domain entities?
