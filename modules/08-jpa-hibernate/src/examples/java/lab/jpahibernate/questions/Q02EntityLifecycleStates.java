package lab.jpahibernate.questions;

import java.util.Map;

public class Q02EntityLifecycleStates {

    public enum EntityState {
        TRANSIENT, // instantiated in Java via new, not associated with session, no DB identity
        MANAGED, // attached to PersistenceContext, tracked by dirty checking, has DB identity
        DETACHED, // has DB identity, but PersistenceContext closed or detached via detach()/clear()
        REMOVED // scheduled for deletion upon flush/commit
    }

    public static void main(String[] args) {
        Map<String, EntityState> transitions =
                Map.of(
                        "new Entity()", EntityState.TRANSIENT,
                        "em.persist(e)", EntityState.MANAGED,
                        "em.detach(e) / em.close()", EntityState.DETACHED,
                        "em.remove(e)", EntityState.REMOVED);

        boolean dirtyCheckingAppliesToTransient = false; // false
        boolean dirtyCheckingAppliesToManaged = true; // true
        boolean dirtyCheckingAppliesToDetached = false; // false

        System.out.println(
                "Transient state: "
                        + transitions.get("new Entity()")); // Transient state: TRANSIENT
        System.out.println(
                "Managed state: " + transitions.get("em.persist(e)")); // Managed state: MANAGED
        System.out.println(
                "Detached state: "
                        + transitions.get("em.detach(e) / em.close()")); // Detached state: DETACHED
        System.out.println(
                "Dirty checking on managed: "
                        + dirtyCheckingAppliesToManaged); // Dirty checking on managed: true
        System.out.println(
                "Dirty checking on detached: "
                        + dirtyCheckingAppliesToDetached); // Dirty checking on detached: false
        System.out.println(
                "Dirty checking on transient: "
                        + dirtyCheckingAppliesToTransient); // Dirty checking on transient: false
    }
}
