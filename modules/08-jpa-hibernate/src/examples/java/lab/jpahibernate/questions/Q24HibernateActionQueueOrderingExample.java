package lab.jpahibernate.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q24HibernateActionQueueOrderingExample {
    private Q24HibernateActionQueueOrderingExample() {}

    // Hibernate ActionQueue execution order during session flush:
    // 1. EntityInsertAction
    // 2. EntityUpdateAction
    // 3. CollectionRemoveAction
    // 4. CollectionUpdateAction
    // 5. CollectionRecreateAction
    // 6. EntityDeleteAction
    //
    // Critical pitfall: Because Inserts execute BEFORE Deletes, if you remove an entity with a
    // UNIQUE key
    // and insert a new entity with the same UNIQUE key in the same transaction, Hibernate attempts
    // the INSERT before the DELETE, triggering a Unique Constraint Violation in the database!
    public static class ActionQueueOrderSimulator {
        public static List<String> simulateFlushOrder() {
            List<String> executionOrder = new ArrayList<>();
            // User calls: em.remove(oldUser); em.persist(newUser);
            // ActionQueue queues actions and executes in fixed order:
            executionOrder.add("INSERT newUser"); // Action 1
            executionOrder.add("DELETE oldUser"); // Action 6
            return executionOrder;
        }
    }

    public static void main(String[] args) {
        List<String> order = ActionQueueOrderSimulator.simulateFlushOrder();
        // Insert runs before delete!
        boolean insertFirst = order.getFirst().startsWith("INSERT"); // true
        // Fix: Call em.flush() explicitly between em.remove(oldUser) and em.persist(newUser)
    }
}
