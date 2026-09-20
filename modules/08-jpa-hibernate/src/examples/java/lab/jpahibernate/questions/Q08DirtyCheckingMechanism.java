package lab.jpahibernate.questions;

import java.util.Arrays;

public class Q08DirtyCheckingMechanism {

    public static void main(String[] args) {
        // When an entity is loaded into the PersistenceContext, Hibernate captures an initial
        // loaded snapshot.
        Object[] loadedSnapshot = new Object[] {1L, "alice@example.com", "Alice"};
        // When property is mutated, currentState differs from loadedSnapshot
        Object[] currentState = new Object[] {1L, "alice@example.com", "Alice Smith"};

        boolean isDirty = !Arrays.equals(loadedSnapshot, currentState); // true

        // Dirty checking triggers an UPDATE statement on flush without needing explicit em.merge()
        // or em.update()
        boolean requiresExplicitSaveCall = false; // false

        System.out.println("Entity is dirty: " + isDirty); // Entity is dirty: true
        System.out.println(
                "Requires explicit save() call: "
                        + requiresExplicitSaveCall); // Requires explicit save() call: false
    }
}
